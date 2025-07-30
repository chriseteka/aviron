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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.events.Event;
import com.github.jlangch.aviron.impl.test.TempFS;
import com.github.jlangch.aviron.util.junit.EnableOnMac;


class FileWatcherTest {

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirOnly_NoFiles() {
        final TempFS tempFS = new TempFS();
        
        try {
            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();
 
            final Path mainDir = tempFS.getScanDir().toPath();
            
            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                              mainDir,
                                              true,
                                              e -> { if (e.isRegularFile()) files.offer(e); },
                                              e -> errors.offer(e),
                                              e -> terminations.offer(e),
                                              null,
                                              "/opt/homebrew/bin/fswatch")) {

                fw.start();

                assertEquals(1, fw.getRegisteredPaths().size());
                assertEquals(mainDir, fw.getRegisteredPaths().get(0));

                sleep(1);
            }

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirWithSubDirs_NoFiles() {
        final TempFS tempFS = new TempFS();
        
        try {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();
 
            final Path mainDir = tempFS.getScanDir().toPath();
            
            try(final IFileWatcher fw = new FileWatcher_FsWatch(
							                    mainDir,
							                    true,
	                                            e -> { if (e.isRegularFile()) files.offer(e); },
							                    e -> errors.offer(e),
							                    e -> terminations.offer(e),
							                    null,
							                    "/opt/homebrew/bin/fswatch")) {

                fw.start();

                assertEquals(1, fw.getRegisteredPaths().size());
                assertEquals(mainDir, fw.getRegisteredPaths().get(0));

                sleep(1);
            }

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirOnly() {
        final TempFS tempFS = new TempFS();
        
        try {
            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
							                    mainDir,
							                    true,
	                                            e -> { if (e.isRegularFile()) files.offer(e); },
							                    e -> errors.offer(e),
							                    e -> terminations.offer(e),
							                    null,
							                    "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

                tempFS.createScanFile("test2.data", "TEST");
                tempFS.createScanFile("test3.data", "TEST");
                tempFS.createScanFile("test4.data", "TEST");

                sleep(2);
            }

            // analyze the generated events

            assertEquals(3, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
        finally {
            tempFS.remove();
        }
    }


    private void sleep(final int seconds) {
        try { Thread.sleep(seconds * 1000); } catch(Exception ex) {}
    }

}
