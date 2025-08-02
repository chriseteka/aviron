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
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.RealtimeScanEvent;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcherQueue;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchTerminationEvent;
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
           final IFileWatcher watcher,
           final Predicate<FileWatchFileEvent> scanApprover,
           final Consumer<RealtimeScanEvent> scanListener,
           final int sleepTimeSecondsOnIdle,
           final boolean testMode
    ) {
        if (client == null) {
            throw new IllegalArgumentException("A 'client' must not be null!");
        }
        if (watcher == null) {
            throw new IllegalArgumentException("A 'fileWatcher' must not be null!");
        }

        this.client = client;
        this.watcher = watcher;
        this.scanApprover = scanApprover;
        this.scanListener = scanListener;
        this.sleepTimeSecondsOnIdle = Math.max(1, sleepTimeSecondsOnIdle);
        this.testMode = testMode;

        watcher.setFileListener(this::onFileEvent);
        watcher.setErrorListener(this::onErrorEvent);
        watcher.setTerminationListener(this::onTerminationEvent);
    }


    protected String name() {
        return "RealtimeScanner";
    }

    protected void onStart() {
        // create a file watcher queue to decouple this scanner from
        // the file watcher
        fileWatcherQueue.set(new FileWatcherQueue(MAX_QUEUE_SIZE));

        try {
           watcher.start();
        }
        catch(Exception ex) {
            throw new FileWatcherException(
                    String.format(
                            "Failed to start FileWatcher on dir '%s'",
                            watcher.getMainDir().toString()),
                    ex);
        }

        startServiceThread(createWorker());
    }

    protected void onClose() throws IOException{
        // stop realtime scanner
        if (watcher.getStatus() == ServiceStatus.RUNNING) {
           watcher.close();
        }
    }

    private Runnable createWorker() {
        return () -> {
            final FileWatcherQueue queue = fileWatcherQueue.get();

            while (isInRunningState()) {
                try {
                    for(int ii=0; ii<BATCH_SIZE && isInRunningState(); ii++) {
                        final File file = queue.pop();
                        if (file != null) {
                           scanFile(file);
                        }
                        else {
                            break;
                        }
                    }

                    if (queue.isEmpty()) {
                        // idle sleep
                        for(int ii=0; ii<sleepTimeSecondsOnIdle && isInRunningState(); ii++) {
                            sleep(1000);
                        }
                    }
                }
                catch(Exception ex) {
                    if (errorCount.incrementAndGet() > MAX_ERROR_COUNT) {
                        throw new FileWatcherException(
                                "Realtime file scanner exceeded the max error count! Scanning stopped!");
                    }
                    // prevent thread spinning in fatal error conditions
                    sleep(5_000);
                }
            }
        };
    }

    private void scanFile(final File file) {
        if (file != null && file.isFile()) {
            final Path path = file.toPath();

            final RealtimeScanEvent event;
            if (testMode) {
                event = new RealtimeScanEvent(path, ScanResult.ok(), testMode);
            }
            else {
                final ScanResult result = client.scan(path);
                event = new RealtimeScanEvent(path, result, testMode);
            }

            // publish event
            if (scanListener != null) {
                safeRun(() -> scanListener.accept(event));
            }
        }
    }

    private void onFileEvent(final FileWatchFileEvent event) {
        final FileWatcherQueue queue = fileWatcherQueue.get();

        // we are not interested in directories just in files
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

    private void onErrorEvent(final FileWatchErrorEvent event) {
        
    }

    private void onTerminationEvent(final FileWatchTerminationEvent event) {
        
    }

    private void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


    private static final int BATCH_SIZE = 100;
    private static final int MAX_QUEUE_SIZE = 5000;
    private static final int MAX_ERROR_COUNT = 1000;

    private final Client client;
    private final IFileWatcher watcher;
    private final Predicate<FileWatchFileEvent> scanApprover;
    private final Consumer<RealtimeScanEvent> scanListener;
    private final int sleepTimeSecondsOnIdle;
    private final boolean testMode;

    private AtomicLong errorCount = new AtomicLong();
    private AtomicReference<FileWatcherQueue> fileWatcherQueue = new AtomicReference<>();
}
