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
package com.github.jlangch.aviron.filewatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.events.FileWatchFileEvent;
import com.github.jlangch.aviron.events.FileWatchFileEventType;
import com.github.jlangch.aviron.events.FileWatchTerminationEvent;
import com.github.jlangch.aviron.util.service.Service;


/**
 * A FileWatcher based on the Java WatchService
 * 
 * <p>Note: The Java WatchService does not work properly on MacOS!
 */
public class FileWatcher_JavaWatchService extends Service implements IFileWatcher {

    public FileWatcher_JavaWatchService(
            final Path mainDir,
            final boolean registerAllSubDirs
    ) {
        this(mainDir, registerAllSubDirs, null, null, null);
    }

    public FileWatcher_JavaWatchService(
            final Path mainDir,
            final boolean registerAllSubDirs,
            final Consumer<FileWatchFileEvent> fileListener,
            final Consumer<FileWatchErrorEvent> errorListener,
            final Consumer<FileWatchTerminationEvent> terminationListener
    ) {
        if (mainDir == null) {
            throw new IllegalArgumentException("The mainDir must not be null!");
        }
        if (!Files.isDirectory(mainDir)) {
            throw new RuntimeException("The main dir " + mainDir + " does not exist or is not a directory");
        }

        this.mainDir = mainDir.toAbsolutePath().normalize();
        this.fileListener.set(fileListener);
        this.errorListener.set(errorListener);
        this.terminationListener.set(terminationListener);

        try {
            this.ws = mainDir.getFileSystem().newWatchService();

            if (registerAllSubDirs) {
                Files.walk(mainDir)
                     .filter(Files::isDirectory)
                     .forEach(this::register);
            }
            else {
                register(mainDir);
            }
        }
        catch(Exception ex) {
            throw new RuntimeException("Failed to create FileWatcher!", ex);
        }
    }

    @Override
    public Path getMainDir() {
        return mainDir;
    }

    @Override
    public List<Path> getRegisteredPaths() {
        return keys.values().stream().sorted().collect(Collectors.toList());
    }

    @Override
    public void setFileSelector(final Predicate<FileWatchFileEvent> selector) {
        fileSelector.set(selector);
    }

    @Override
    public void setFileListener(final Consumer<FileWatchFileEvent> listener) {
        fileListener.set(listener);
    }

    @Override
    public void setErrorListener(final Consumer<FileWatchErrorEvent> listener) {
        errorListener.set(listener);
    }

    @Override
    public void setTerminationListener(final Consumer<FileWatchTerminationEvent> listener) {
        terminationListener.set(listener);
    }

    protected String name() {
        return "FileWatcher_JavaWatchService";
    }

    protected void onStart() {
        startServiceThread(createWorker());

        // spin wait max 5s for service after creation to be ready or closed
        waitForServiceStarted(5);
    }

    protected void onClose() throws IOException {
        ws.close();

        fireEvent(new FileWatchTerminationEvent(mainDir));
    }


    private void register(final Path dir) {
        try {
            final Path normalizedDir = dir.toAbsolutePath().normalize();

            final WatchKey dirKey = normalizedDir.register(
                                      ws,
                                      ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            keys.put(dirKey, normalizedDir);
        }
        catch(Exception e) {
            fireEvent(new FileWatchErrorEvent(dir, e));
        }
    }

    private Runnable createWorker() {
        return () -> {
            enteredRunningState();

            while (isInRunningState()) {
                try {
                    final WatchKey key = ws.take();
                    if (key == null) {
                        break;
                    }

                    final Path dirPath = keys.get(key);
                    if (dirPath == null) {
                        continue;
                    }

                    key.pollEvents()
                       .stream()
                       .filter(e -> e.kind() != OVERFLOW)
                       .forEach(e -> {
                            @SuppressWarnings("unchecked")
                            final Path p = ((WatchEvent<Path>)e).context();
                            final Path absPath = dirPath.resolve(p);
                            final FileWatchFileEventType eventType = convertToEventType(e.kind());
                            if (Files.isDirectory(absPath)) {
                                if (eventType == FileWatchFileEventType.CREATED) {
                                    // dynamically register the new subdir with the Java WatchService
                                    register(absPath);
                                }

                                // send a dir created event
                                if (eventType != FileWatchFileEventType.CREATED) {
                                    fireEvent(new FileWatchFileEvent(absPath, true, false, eventType));
                                }
                            }
                            else if (Files.isRegularFile(absPath)) {
                                fireEvent(new FileWatchFileEvent(absPath, false, true, eventType));
                            }
                            else {
                                // if the file has been deleted its type cannot be checked
                                fireEvent(new FileWatchFileEvent(absPath, false, false, eventType));
                            }});

                    key.reset();
                }
                catch(ClosedWatchServiceException ex) {
                    break; // stop watching
                }
                catch(InterruptedException ex) {
                    break; // stop watching
                }
                catch(Exception ex) {
                    fireEvent(new FileWatchErrorEvent(mainDir, ex));
                    // continue processing events
                }
            }

            if (!isInClosedState()) {
                close();
            }
        };
    }

    private void fireEvent(final FileWatchFileEvent event) {
        final Predicate<FileWatchFileEvent> selector = fileSelector.get();
        if (selector == null || selector.test(event)) {
            final Consumer<FileWatchFileEvent> listener = fileListener.get();
            if (listener != null) {
                safeRun(() -> listener.accept(event));
            }
        }
    }

    private void fireEvent(final FileWatchErrorEvent event) {
        final Consumer<FileWatchErrorEvent> listener = errorListener.get();
        if (listener != null) {
            safeRun(() -> listener.accept(event));
        }
    }

    private void fireEvent(final FileWatchTerminationEvent event) {
        final Consumer<FileWatchTerminationEvent> listener = terminationListener.get();
        if (listener != null) {
            safeRun(() -> listener.accept(event));
        }
    }

    private static void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }

    private FileWatchFileEventType convertToEventType(final WatchEvent.Kind<?> kind) {
        if (kind == null) {
            return null;
        }
        else {
            switch(kind.name()) {
                case "ENTRY_CREATE": return FileWatchFileEventType.CREATED;
                case "ENTRY_DELETE": return FileWatchFileEventType.DELETED;
                case "ENTRY_MODIFY": return FileWatchFileEventType.MODIFIED;
                case "OVERFLOW":     return FileWatchFileEventType.OVERFLOW;
                default:             return null;
            }
        }
    }


    private final Path mainDir;
    private final WatchService ws;
    private final Map<WatchKey,Path> keys = new HashMap<>();
    private final AtomicReference<Predicate<FileWatchFileEvent>> fileSelector = new AtomicReference<>();
    private final AtomicReference<Consumer<FileWatchFileEvent>> fileListener = new AtomicReference<>();
    private final AtomicReference<Consumer<FileWatchErrorEvent>> errorListener = new AtomicReference<>();
    private final AtomicReference<Consumer<FileWatchTerminationEvent>> terminationListener = new AtomicReference<>();
}
