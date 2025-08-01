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
        try(TempFS tempFS = new TempFS()) {
            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = tempFS.getScanDir().toPath();

            try(final IFileWatcher fw = new FileWatcher_FsWatch(
                                              mainDir,
                                              true,
                                              e -> { if (e.isFile()) {
                                                        printf("File Event: %s %s%n", e.getPath(), e.getType());
                                                        files.offer(e); 
                                                   }},
                                              e -> { printf("Error:        %s%n", e.getPath());
                                                     errors.offer(e); },
                                              e -> { printf("Terminated:   %s%n", e.getPath());
                                                     terminations.offer(e); },
                                              null,
                                              null, // default platform monitor
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
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirWithSubDirs_NoFiles() {
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
                                                          printf("File Event: %s %s%n", e.getPath(), e.getType());
                                                          files.offer(e); 
                                                    }},
                                                e -> { printf("Error:        %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated:   %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null,
                                                null, // default platform monitor
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
    }

    @Test 
    @EnableOnMac
    void testFileWatcherMainDir() {
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
                                                           printf("File Event: %s %s%n", e.getPath(), e.getType());
                                                           files.offer(e); 
                                                       }},
                                                e -> { printf("Error:        %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated:   %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null,
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

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

            // wait to receive the termination even
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
                                                           printf("File Event: %s %s%n", e.getPath(), e.getType());
                                                           files.offer(e); 
                                                       }},
                                                e -> { printf("Error:        %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated:   %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null,
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

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

            // wait to receive the termination even
            sleep(1);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherSubDir_DynaicallyAdded() {
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
                                                           printf("File Event: %s %s%n", e.getPath(), e.getType());
                                                           files.offer(e); 
                                                       }},
                                                e -> { printf("Error:        %s%n", e.getPath());
                                                       errors.offer(e); },
                                                e -> { printf("Terminated:   %s%n", e.getPath());
                                                       terminations.offer(e); },
                                                null,
                                                null, // default platform monitor
                                                "/opt/homebrew/bin/fswatch")) {

                fw.start();

                sleep(1);

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

            // wait to receive the termination even
            sleep(1);

            // analyze the generated events

            assertEquals(6, files.size());
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
