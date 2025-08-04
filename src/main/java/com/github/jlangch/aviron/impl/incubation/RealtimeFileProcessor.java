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
package com.github.jlangch.aviron.impl.incubation;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.github.jlangch.aviron.events.RealtimeScanEvent;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcherQueue;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.util.service.Service;
import com.github.jlangch.aviron.util.service.ServiceStatus;


// +--------------------------------------------------------------------------+
// |                                                                          |
// |                NOT YET TESTED    -->   DO NOT USE                        |
// |                                                                          |
// +--------------------------------------------------------------------------+

/**
 * Realtime file processor
 * 
 * <p>The RealtimeFileProcessor orchestrates the FileWatcher and the 
 * FileWatcherQueue to deliver file scan events to an AV client. It applies
 * some optimization to file watching events on deleted files. It helps
 * keep the scan pipeline safe and sound even if the pipeline is overrun by 
 * file watching events.
 * 
 * <pre>
 * 
 * +------------+   +--------------------------------+   +-----------+   +-------+
 * | Filesystem | ⇨ | ⇘  Realtime File Processor   ⇗ | ⇨ | AV Client | ⇨ | Clamd |
 * +------------+   |  ⇓                          ⇑  |   +-----------+   +-------+
 *                  | +-------------+             ⇑  |                            
 *                  | | FileWatcher | ⇨ |  Queue   | |                            
 *                  | +-------------+   +----------+ |                            
 *                  +--------------------------------+                            
 * 
 * </pre>
 *
 * <p>The FileWatcherQueue is buffering file watching events. It asynchronously 
 * decouples the event producing FileWatcher from the event consuming AV scanner 
 * client.
 * 
 * <p>The FileWatcherQueue never blocks and never grows beyond limits to protect
 * the system! Therefore the queue is non blocking and has a fix capacity. As a
 * consequence it must discard old events if overrun. 
 *
 * <p>The AV Client might not keep up scanning files with the event producing
 * files system. The FileWatcherQueue prevents such a situation.
 * 
 * <p>File watchers (like the Java WatchService or the 'fswatch' tool) have the 
 * same behavior. If they get overrun with file change events they discard events 
 * and signal it by sending an 'OVERFLOW' event to their clients.
 */

public class RealtimeFileProcessor extends Service {

    public RealtimeFileProcessor(
           final IFileWatcher watcher,
           final int sleepTimeSecondsOnIdle,
           final Consumer<RealtimeScanEvent> scanListener,
           final Consumer<FileWatchErrorEvent> errorListener
    ) {
        if (watcher == null) {
            throw new IllegalArgumentException("A 'fileWatcher' must not be null!");
        }

        this.watcher = watcher;
        this.sleepTimeSecondsOnIdle = Math.max(1, sleepTimeSecondsOnIdle);
        this.scanListener = scanListener;

        watcher.setFileListener(this::onFileEvent);
        watcher.setErrorListener(errorListener);
        watcher.setTerminationListener(null);
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
            
            enteredRunningState();

            while (isInRunningState()) {
                try {
                    for(int ii=0; ii<BATCH_SIZE && isInRunningState(); ii++) {
                        final File file = queue.pop(true);
                        if (file != null && file.isFile()) {
                            fireEvent(new RealtimeScanEvent(file.toPath()));
                        }
                    }

                    if (queue.isEmpty()) {
                        // idle sleep
                        if (sleepTimeSecondsOnIdle == 0 && isInRunningState()) {
                            sleep(100);    
                        }
                        else {
                            for(int ii=0; ii<sleepTimeSecondsOnIdle && isInRunningState(); ii++) {
                                sleep(1000);
                            }
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

    private void onFileEvent(final FileWatchFileEvent event) {
        final FileWatcherQueue queue = fileWatcherQueue.get();

        // we are not interested in directories but in new or modified files
        if (event.isFile()) {
            switch(event.getType()) {
                case CREATED:
                case MODIFIED:
                    queue.push(event.getPath().toFile());
                    break;

                case DELETED:
                    // optimization: if there is already a file with this path in
                    //               the queue, remove it from the queue, because
                    //               it has now been deleted!
                    queue.remove(event.getPath().toFile());
                    break;

                case OVERFLOW:
                default:
                    break;
            }
        }
    }

    private void fireEvent(final RealtimeScanEvent event) {
        if (scanListener != null) {
            safeRun(() -> scanListener.accept(event));
        }
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

    private final IFileWatcher watcher;
    private final Consumer<RealtimeScanEvent> scanListener;
    private final int sleepTimeSecondsOnIdle;

    private AtomicLong errorCount = new AtomicLong();
    private AtomicReference<FileWatcherQueue> fileWatcherQueue = new AtomicReference<>();
}
