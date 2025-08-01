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
package com.github.jlangch.aviron.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.events.Event;
import com.github.jlangch.aviron.impl.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.impl.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.impl.test.TempFS;
import com.github.jlangch.aviron.util.junit.EnableOnMac;


class FileWatcherTest {

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirOnly_NoFiles() {
        printf("%n%n[testFileWatcherMainDirOnly_NoFiles]%n%n");

        try(TempFS tempFS = new TempFS()) {
            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                              mainDir,
                                              true,
                                              e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                              e -> { printf("Error:      %s%n", e.getPath());
                                                     errors.offer(e); },
                                              e -> { printf("Terminated: %s%n", e.getPath());
                                                     terminations.offer(e); },
                                              null, // default platform monitor
                                              "/opt/homebrew/bin/fswatch")) {

                fw.start();

                assertEquals(1, fw.getRegisteredPaths().size());
                assertEquals(mainDir, fw.getRegisteredPaths().get(0));

                sleep(1);

                printf("Ready to watch%n%n");
            }

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirWithSubDirs_NoFiles() {
        printf("%n%n[testFileWatcherMainDirWithSubDirs_NoFiles]%n%n");

        try(TempFS tempFS = new TempFS()) {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                                mainDir,
                                                true,
                                                e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                                e -> { printf("Error:      %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated: %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                assertEquals(1, fw.getRegisteredPaths().size());
                assertEquals(mainDir, fw.getRegisteredPaths().get(0));

                sleep(1);

                printf("Ready to watch%n%n");
           }

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDir() {
        printf("%n%n[testFileWatcherMainDir]%n%n");

        try(TempFS tempFS = new TempFS()) {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                                mainDir,
                                                true,
                                                e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                                e -> { printf("Error:      %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated: %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                tempFS.touchScanFile("test1.data");            // created
                sleep(1);  
                tempFS.appendScanFile("test1.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("test1.data");           // deleted
                sleep(1);


                tempFS.createScanFile("test2.data", "TEST");   // modified
                sleep(1);
                tempFS.appendScanFile("test2.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("test2.data");           // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3);
            }

            // wait to receive the termination event
            sleep(1);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherSubDir() {
        printf("%n%n[testFileWatcherSubDir]%n%n");

        try(TempFS tempFS = new TempFS()) {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                                mainDir,
                                                true,
                                                e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                                e -> { printf("Error:      %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated: %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                tempFS.touchScanFile("0000", "test1.data");            // created
                sleep(1);
                tempFS.appendScanFile("0000", "test1.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("0000", "test1.data");           // deleted
                sleep(1);


                tempFS.createScanFile("0001", "test2.data", "TEST");   // modified
                sleep(1);
                tempFS.appendScanFile("0001", "test2.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("0001", "test2.data");           // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3);
            }

            // wait to receive the termination event
            sleep(1);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherSubDir_DynamicallyAdded() {
        printf("%n%n[testFileWatcherSubDir_DynamicallyAdded]%n%n");

        try(TempFS tempFS = new TempFS()) {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                                mainDir,
                                                true,
                                                e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                                e -> { printf("Error:      %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated: %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                tempFS.touchScanFile("0000", "test1.data");            // created
                sleep(1);
                tempFS.appendScanFile("0000", "test1.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("0000", "test1.data");           // deleted
                sleep(1);

                // a new subdir "0002" arrives
                tempFS.createScanSubDir("0002");

                tempFS.touchScanFile("0002", "test3.data");            // created
                sleep(1);
                tempFS.appendScanFile("0002", "test3.data", "TEST");   // modified
                sleep(1);
                tempFS.deleteScanFile("0002", "test3.data");           // deleted
                sleep(1);

                // wait for all events to be processed before closing the watcher
                sleep(3);
            }

            // wait to receive the termination event
            sleep(1);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }


    @Test 
    @EnableOnMac
    void testFileWatcherSubDir_DynamicSubDirs() {
        printf("%n%n[testFileWatcherSubDir_DynamicSubDirs]%n%n");

        try(TempFS tempFS = new TempFS()) {
            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                                mainDir,
                                                true,
                                                e -> { if (e.isFile()) {
                                                        printf("File Event: %-8s %s%n", e.getType(), e.getPath());
                                                        files.offer(e);
                                                     }
                                                     else if (e.isDir()) {
                                                         printf("Dir Event:  %-8s %s%n", e.getType(), e.getPath());
                                                     }},
                                                e -> { printf("Error:      %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated: %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

                printf("Ready to watch%n%n");

                final File dir1 = tempFS.createScanSubDir("0002");
                sleep(1);

                final File dir2 = tempFS.createScanSubDir("0003");
                sleep(1);
                
                dir1.delete();
                sleep(1);
                
                dir2.delete();
                sleep(1);
                
                // wait for all events to be processed before closing the watcher
                sleep(3);
            }

            // wait to receive the termination event
            sleep(1);

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    private void printf(final String format, final Object... args) {
        synchronized(lock) {
            System.out.printf(format, args);
        }
    }

    private void sleep(final int seconds) {
        try { Thread.sleep(seconds * 1000); } catch(Exception ex) {}
    }


    private final Object lock = new Object();
}
