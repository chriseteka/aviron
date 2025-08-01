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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcherQueue;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchTerminationEvent;
import com.github.jlangch.aviron.impl.util.OS;
import com.github.jlangch.aviron.util.service.Service;
import com.github.jlangch.aviron.util.service.ServiceStatus;


/**
 * 
 * The demo filestore layout:
 * 
 * <pre>
 * /data/filestore/
 *   |
 *   +-- 0000
 *   |     \_ file1.doc
 *   |     \_ file2.doc
 *   |     :
 *   |     \_ fileN.doc
 *   +-- 0001
 *   |     \_ file1.doc
 *   |     :
 *   |     \_ fileN.doc
 *   :
 *   +-- NNNN
 *         \_ file1.doc
 * </pre>
 * 
 * <p>A realtime file scanner that scans all newly created docx, xlsx, and pdf 
 * files in filestore with the above layout.
 * 
 * <p>If any new directories appear in the filestore, the file watcher adds it
 * implicitly to its list of watch dirs.
 * 
 * <pre>
 * Client avClient = Client.builder()
 *                       .serverHostname("localhost")
 *                       .serverFileSeparator(FileSeparator.UNIX)
 *                       .build();
 *
 * final Predicate-&lt;FileWatchFileEvent-&gt; scanApprover = 
 *         (e) -&gt; { final String filename = e.getPath().toFile().getName();
 *                  return e.getType() == FileWatchEventType.CREATED
 *                         &amp;&amp; filename.matches(".*[.](docx|xlsx|pdf)")); };
 *
 * final Consumer-&lt;RealtimeScanEvent-&gt; scanListener =
 *         (e) -&gt; { if (e.hasVirus) {
 *                     System.out.println("Infected -&gt; " + e.getPath());
 *                  } };
 *
 * final RealtimeScanner rts = new RealtimeScanner(
 *                                      avClient,
 *                                      Paths.get("/data/filestore/"),
 *                                      true, // include all subdirs
 *                                      scanApprover,
 *                                      scanListener,
 *                                      10);
 *
 * rts.start();
 *
 * rts.close();
 * </pre>
 */
public class RealtimeScanner extends Service {

    public RealtimeScanner(
           final Client client,
           final Path mainDir,
           final boolean registerAllSubDirs,
           final Predicate<FileWatchFileEvent> scanApprover,
           final Consumer<RealtimeScanEvent> scanListener,
           final int sleepTimeOnIdle
    ) {
        if (client == null) {
            throw new IllegalArgumentException("A 'client' must not be null!");
        }
        if (mainDir == null) {
            throw new IllegalArgumentException("A 'mainDir' must not be null!");
        }
        if (!Files.isDirectory(mainDir)) {
            throw new IllegalArgumentException(
                    "The realtime scanner 'mainDir' is not an existing directory!");
        }

        this.client = client;
        this.mainDir = mainDir;
        this.registerAllSubDirs = registerAllSubDirs;
        this.scanApprover = scanApprover;
        this.scanListener = scanListener;
        this.sleepTimeOnIdle = Math.max(1, sleepTimeOnIdle);
    }


    protected String name() {
        return "RealtimeScanner";
    }

    protected void onStart() {
        fileWatcherQueue.set(new FileWatcherQueue(MAX_QUEUE_SIZE));

        try {
            final IFileWatcher fw = createPlatformFileWatcher();
            fw.start();
            watcher.set(fw);
        }
        catch(Exception ex) {
            throw new FileWatcherException(
                    String.format(
                            "Failed to start FileWatcher on dir '%s'",
                            mainDir.toString()),
                    ex);
        }

        startServiceThread(createWorker());
    }

    protected void onClose() throws IOException{
        // stop realtime scanner
        final IFileWatcher fw = watcher.get();
        if (fw != null && fw.getStatus() == ServiceStatus.RUNNING) {
           fw.close();
        }
    }

    private IFileWatcher createPlatformFileWatcher() {
        if (OS.isLinux()) {
            return new FileWatcher_JavaWatchService(
                         mainDir,
                         registerAllSubDirs,
                         this::fileWatchEventListener,
                         this::errorEventListener,
                         this::terminationEventListener);
        }
        else if (OS.isMacOSX()) {
            return new FileWatcher_FsWatch(
                         mainDir,
                         registerAllSubDirs,
                         this::fileWatchEventListener,
                         this::errorEventListener,
                         this::terminationEventListener,
                         null, // default monitor for MacOS platform
                         FileWatcher_FsWatch.HOMEBREW_FSWATCH_PROGRAM);
        }
        else {
            throw new FileWatcherException(
                    "FileWatcher is not supported on platforms other than Linux/MacOS!");
        }
    }

    private Runnable createWorker() {
        return () -> {
            final FileWatcherQueue queue = fileWatcherQueue.get();

            while (isInRunningState()) {
                try {
                    for(int ii=0; ii<BATCH_SIZE && isInRunningState(); ii++) {
                        final File file = queue.pop();
                        if (file.isFile()) {
                            final Path path = file.toPath();
                            final ScanResult result = client.scan(path);
                            if (scanListener != null) {
                                safeRun(() -> scanListener.accept(
                                                new RealtimeScanEvent(path, result)));
                            }
                        }
                    }

                    if (queue.isEmpty()) {
                        for(int ii=0; ii<sleepTimeOnIdle && isInRunningState(); ii++) {
                            sleep(500);
                        }
                    }
                }
                catch(Exception ex) {
                    // prevent thread spinning in fatal error conditions
                    sleep(5_000);
                }
            }
        };
    }

    private void fileWatchEventListener(final FileWatchFileEvent event) {
        final FileWatcherQueue queue = fileWatcherQueue.get();

        if (event.isFile()) {
            switch(event.getType()) {
                case CREATED:
                case MODIFIED:
                    try {
                        if (scanApprover == null || scanApprover.test(event)) {
                            queue.push(event.getPath().toFile());
                        }
                    }
                    catch(Exception ex) { }
                    break;

                case DELETED:
                    queue.remove(event.getPath().toFile());
                    break;

                case OVERFLOW:
                    break;

                default:
                    break;
            }
        }
    }

    private void errorEventListener(final FileWatchErrorEvent event) {
        
    }

    private void terminationEventListener(final FileWatchTerminationEvent event) {
        
    }

    private void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


    private static final int BATCH_SIZE = 300;
    private static final int MAX_QUEUE_SIZE = 5000;

    private final Client client;
    private final Path mainDir;
    private final boolean registerAllSubDirs;
    private final Predicate<FileWatchFileEvent> scanApprover;
    private final Consumer<RealtimeScanEvent> scanListener;
    private final int sleepTimeOnIdle;

    private AtomicReference<IFileWatcher> watcher = new AtomicReference<>();
    private AtomicReference<FileWatcherQueue> fileWatcherQueue = new AtomicReference<>();
}
