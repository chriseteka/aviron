/*                 _                 
 *       /\       (_)            
 *      /  \__   ___ _ __ ___  _ __  
 *     / /\ \ \ / / | '__/ _ \| '_ \ 
 *    / ____ \ V /| | | | (_) | | | |
 *   /_/    \_\_/ |_|_|  \___/|_| |_|
 *
 *
 * Copyright 2025 Aviron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jlangch.aviron.impl.filewatcher;

import static com.github.jlangch.aviron.events.FileWatchFileEventType.CREATED;
import static com.github.jlangch.aviron.events.FileWatchFileEventType.DELETED;
import static com.github.jlangch.aviron.events.FileWatchFileEventType.MODIFIED;
import static com.github.jlangch.aviron.events.FileWatchFileEventType.OVERFLOW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.events.FileWatchFileEvent;
import com.github.jlangch.aviron.events.FileWatchFileEventType;
import com.github.jlangch.aviron.events.FileWatchRegisterEvent;
import com.github.jlangch.aviron.events.FileWatchTerminationEvent;
import com.github.jlangch.aviron.impl.service.Service;
import com.github.jlangch.aviron.impl.util.CollectionUtils;


/**
 * FileWatcher implementation based on top of the <i>fswatch</i> tool.
 *
 * <p>The Java WatchService does not work properly on MacOS. This FileWatcher
 * solves the problem on MacOS.
 *
 * <p><i>fswatch</i> is installed via Homebrew:
 *
 * <pre>
 *   brew install fswatch
 * </pre>
 *
 *
 * @see <a href="https://github.com/emcrisostomo/fswatch/">fswatch Github</a>
 * @see <a href="https://emcrisostomo.github.io/fswatch/doc/1.17.1/fswatch.html/">fswatch Manual</a>
 * @see <a href="https://formulae.brew.sh/formula/fswatch">fswatch Installation</a>
 */
public class FileWatcher_FsWatch extends Service implements IFileWatcher {

    public FileWatcher_FsWatch(
            final Path mainDir,
            final boolean recursive,
            final Consumer<FileWatchFileEvent> fileListener,
            final Consumer<FileWatchErrorEvent> errorListener,
            final Consumer<FileWatchTerminationEvent> terminationListener,
            final Consumer<FileWatchRegisterEvent> registerListener,
            final FsWatchMonitor monitor,
            final String fswatchProgram
    ) {
        if (mainDir == null) {
            throw new IllegalArgumentException("The mainDir must not be null!");
        }
        if (!Files.isDirectory(mainDir)) {
            throw new RuntimeException("The main dir " + mainDir + " does not exist or is not a directory");
        }
        if (fswatchProgram != null && !Files.isExecutable(Paths.get(fswatchProgram))) {
            throw new IllegalArgumentException("The fswatch Program does not exist or is not executable!");
        }

        this.mainDir = mainDir.toAbsolutePath().normalize();
        this.recursive = recursive;
        this.fileListener = fileListener;
        this.registerListener = registerListener;
        this.errorListener = errorListener;
        this.terminationListener = terminationListener;
        this.monitor = monitor;
        this.fswatchProgram = fswatchProgram == null ? "fswatch" : fswatchProgram;
    }

    @Override
    public Path getMainDir() {
        return mainDir;
    }

    @Override
    public List<Path> getRegisteredPaths() {
        return CollectionUtils.toList(mainDir);
    }


    protected String name() {
        return "FileWatcher_FsWatch";
    }

    protected void onStart() {
        // fswatch options:
        // -0                  null-separated output
        // -r                  recursive
        // --fire-idle-event   fire idle events
        // --allow-overflow    allow a monitor to overflow and report it as a change event
        // --event-flags       include event flags like Created, Updated, etc

        final List<String> options = new ArrayList<>();
        options.add(fswatchProgram);
        options.add("--format=%p" + SEPARATOR + "%f");
        if (monitor != null) options.add("--monitor=" + monitor.name());
        if (recursive) options.add("-r");
        options.add(mainDir.toString());

        final ProcessBuilder pb = new ProcessBuilder(options.toArray(new String[] {}));
        pb.redirectErrorStream(true);

        try {
            // start the fswatch process
            fswatchProcess.set(pb.start());
        }
        catch(Exception ex) {
            throw new RuntimeException("Failed to start 'fswatch' process", ex);
        }

        startServiceThread(createWorker());

        // spin wait max 5s for service to be ready or closed
        waitForServiceStarted(5);
    }

    protected void onClose() throws IOException{
        final Process process = fswatchProcess.get();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }

        if (terminationListener != null) {
            safeRun(() -> terminationListener.accept(
                            new FileWatchTerminationEvent(mainDir)));
        }
    }


    private Runnable createWorker() {
        return () -> {
            try {
                final Process process = fswatchProcess.get();

                try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(process.getInputStream()))) {

                    final StringBuilder buffer = new StringBuilder();

                    enteredRunningState();

                    int ch;
                    while ((ch = reader.read()) != -1 && isInRunningState()) {
                        if (ch == '\n') { // event terminator
                            final String line = buffer.toString();
                            buffer.setLength(0);

                            if (isIdleEvent(line)) {
                               continue;
                            }

                            // Example line: /path/to/file.txt|#|Updated Created
                            int separatorIdx = line.indexOf(SEPARATOR);
                            if (separatorIdx != -1) {
                                final String filePath = line.substring(0, separatorIdx);
                                final Path path = Paths.get(filePath).normalize();

                                // fswatch is not really helpful with the flags:
                                //
                                // Flags dir  created: Created IsDir AttributeModified
                                // Flags file created: Created IsFile Updated AttributeModified
                                // Flags file updated: Created IsFile Updated AttributeModified
                                // Flags file deleted: Created IsFile Updated Removed AttributeModified
                                final String flags = line.substring(separatorIdx + SEPARATOR.length());
                                final Set<FileWatchFileEventType> types = mapToEventTypes(flags);

                                final boolean isDir = flags.contains("IsDir");
                                final boolean isFile = flags.contains("IsFile");

                                fireFileEvents(path, isDir, isFile, types);
                            }
                            else {
                                // fallback in case of no flags
                                final Path path = Paths.get(line);
                                fireFallbackFileEvents(path);
                            }
                        }
                        else {
                            buffer.append((char) ch);
                        }
                    }
                }
            }
            catch (Exception ex) {
                if (errorListener != null) {
                    safeRun(() -> errorListener.accept(
                                    new FileWatchErrorEvent(mainDir, ex)));
                }
            }

            if (!isInClosedState()) {
                close();
            }
        };
    }

    private void fireFileEvents(
            final Path path,
            final boolean isDir,
            final boolean isFile,
            final Set<FileWatchFileEventType> types
    ) {
        if (isDir) {
            if (types.contains(CREATED)) {
                safeRun(() -> registerListener.accept(
                                    new FileWatchRegisterEvent(path)));
            }

            if (types.contains(CREATED)) {
                safeRun(() -> fileListener.accept(
                                 new FileWatchFileEvent(path, isDir, isFile, CREATED)));
            }
            else if (types.contains(DELETED)) {
                safeRun(() -> fileListener.accept(
                                new FileWatchFileEvent(path, isDir, isFile, DELETED)));
            }
        }
        else if (isFile) {
            if (Files.isRegularFile(path)) {
                if (types.contains(MODIFIED)) {
                    safeRun(() -> fileListener.accept(
                                    new FileWatchFileEvent(path, isDir, isFile, MODIFIED)));
                }
                else if (types.contains(CREATED)) {
                   safeRun(() -> fileListener.accept(
                                    new FileWatchFileEvent(path, isDir, isFile, CREATED)));
                }
                else if (types.contains(DELETED)) {
                    safeRun(() -> fileListener.accept(
                                    new FileWatchFileEvent(path, isDir, isFile, DELETED)));
                }
            }
            else {
                if (types.contains(DELETED)) {
                    safeRun(() -> fileListener.accept(
                                    new FileWatchFileEvent(path, isDir, isFile, DELETED)));
                }
                else {
                    safeRun(() -> fileListener.accept(
                                    new FileWatchFileEvent(path, isDir, isFile, MODIFIED)));
                }
            }
        }
    }

    private void fireFallbackFileEvents(final Path path) {
        if (Files.isDirectory(path)) {
            safeRun(() -> fileListener.accept(
                    new FileWatchFileEvent(path, true, false, MODIFIED)));
        }
        else if (Files.isRegularFile(path)) {
            safeRun(() -> fileListener.accept(
                    new FileWatchFileEvent(path, false, true, MODIFIED)));
        }
        else {
            // if the file has been deleted its type cannot be checked
            safeRun(() -> fileListener.accept(
                    new FileWatchFileEvent(path, false, false, DELETED)));
        }
    }

    private boolean isIdleEvent(final String line) {
        return line.matches(" *NoOp *");
    }

    private Set<FileWatchFileEventType> mapToEventTypes(final String flags) {
       return Arrays.stream(flags.split(" "))
                    .map(s -> mapToEventType(s))
                    .filter(e -> e != null)
                    .collect(Collectors.toSet());
    }

    private FileWatchFileEventType mapToEventType(final String flag) {
        switch(flag) {
            // This event maps a platform-specific event that has no corresponding flag.
            case "PlatformSpecific": return null;

            // The object has been created
            case "Created": return CREATED;

            // The object has been updated. The kind of update is monitor-dependent
            case "Updated": return MODIFIED;

            // The object has been removed
            case "Removed": return DELETED;

            // The object has been renamed
            case "Renamed": return null;

            // The object’s owner has changed
            case "OwnerModified": return null;

            // An object’s attribute has changed
            case "AttributeModified": return null;

            // The object has moved from this location to a new location of the same file system
            case "MovedFrom": return null;

            // The object has moved from another location in the same file system into this location
            case "MovedTo": return null;

            // The object is a regular file
            case "IsFile": return null;

            // The object is a directory
            case "IsDir": return null;

            // The object is a symbolic link
            case "IsSymLink": return null;

            // The object link count has changed
            case "Link": return null;

            // The monitor has overflowed
            case "Overflow": return OVERFLOW;

            default: return null;
        }
    }

    private static void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


    // any reasonable string that does not appear in file names
    private static final String SEPARATOR = "|#|";

    private final AtomicReference<Process> fswatchProcess = new AtomicReference<>();

    private final Path mainDir;
    private final boolean recursive;
    private final Consumer<FileWatchFileEvent> fileListener;
    private final Consumer<FileWatchRegisterEvent> registerListener;
    private final Consumer<FileWatchErrorEvent> errorListener;
    private final Consumer<FileWatchTerminationEvent> terminationListener;
    private final FsWatchMonitor monitor;
    private final String fswatchProgram;
}
