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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.github.jlangch.aviron.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.events.FileWatchEvent;
import com.github.jlangch.aviron.events.FileWatchRegisterEvent;
import com.github.jlangch.aviron.events.FileWatchTerminationEvent;


public class RealtimeFileWatcher {

    public RealtimeFileWatcher(
           final Path mainDir,
           final List<Path> secondaryDirs,
           final Predicate<FileWatchEvent> scanApprover
    ) {
        if (mainDir == null) {
            throw new IllegalArgumentException("A 'mainDir' must not be null!");
        }
        if (!Files.isDirectory(mainDir)) {
            throw new IllegalArgumentException(
                    "The realtime scanner 'mainDir' is not an existing directory!");
        }

        this.mainDir = mainDir;
        if (secondaryDirs != null) {
            this.secondaryDirs.addAll(secondaryDirs);
        }
        this.scanApprover = scanApprover;
    }


    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            // start realtime scanner

            try {
                fileWatcherQueue.set(new FileWatcherQueue(5_000));

                watcher.set(new FileWatcher(
                                    mainDir,
                                    this::fileWatchEventListener,
                                    this::registerEventListener,
                                    this::errorEventListener,
                                    this::terminationEventListener));
            }
            catch(Exception ex) {
                running.set(false);
                throw ex;
            }

            watcher.get().register(secondaryDirs);
        }
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            // stop realtime scanner
            final FileWatcher fw = watcher.get();
            if (fw != null && fw.isRunning()) {
               fw.close();
            }
        }
    }

    public List<File> popNextFiles(final int n) {
        final FileWatcherQueue queue = fileWatcherQueue.get();
        return queue != null ? queue.pop(n) : new ArrayList<>();
    }


    private void fileWatchEventListener(final FileWatchEvent event) {      
        if (isApprovedScan(event)) {
            fileWatcherQueue.get().push(event.getPath().toFile());
        }
    }

    private void registerEventListener(final FileWatchRegisterEvent event) {
        
    }

    private void errorEventListener(final FileWatchErrorEvent event) {
        
    }

    private void terminationEventListener(final FileWatchTerminationEvent event) {
        
    }


    private boolean isApprovedScan(final FileWatchEvent event) {
        try {
            switch(event.getType()) {
                case CREATED:
                    return scanApprover == null || scanApprover.test(event);

                case MODIFIED:
                    return scanApprover == null || scanApprover.test(event);

                case DELETED:
                    return false;

                case OVERFLOW:
                    return false;

                default:
                    return false;
            }
        }
        catch(Exception ex) {
            return false;
        }
    }


    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Path mainDir;
    private final List<Path> secondaryDirs = new ArrayList<>();
    private final Predicate<FileWatchEvent> scanApprover;

    private AtomicReference<FileWatcher> watcher = new AtomicReference<>();
    private AtomicReference<FileWatcherQueue> fileWatcherQueue = new AtomicReference<>();
}
