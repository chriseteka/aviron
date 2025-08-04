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

import static com.github.jlangch.aviron.util.DemoUtil.printf;
import static com.github.jlangch.aviron.util.DemoUtil.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.events.Event;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.junit.EnableOnMac;


class FileWatcherTest {

    @Test 
    @EnableOnMac
    void testFileWatcherMainDirOnly_NoFiles() {
        printf("%n%n[FileWatcherTest::testFileWatcherMainDirOnly_NoFiles]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

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
        printf("%n%n[FileWatcherTest::testFileWatcherMainDirWithSubDirs_NoFiles]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

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
        printf("%n%n[FileWatcherTest::testFileWatcherMainDir]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                demoFS.touchFilestoreFile("000", "test1.data");     // created
                sleep(1000);  
                demoFS.appendToFilestoreFile("000", "test1.data");  // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("000", "test1.data");    // deleted
                sleep(1000);


                demoFS.createFilestoreFile("000", "test2.data");    // modified
                sleep(1000);
                demoFS.appendToFilestoreFile("000", "test2.data");  // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("000", "test2.data");    // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3000);
            }

            // wait to receive the termination event
            sleep(1000);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherSubDir() {
        printf("%n%n[FileWatcherTest::testFileWatcherSubDir]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                demoFS.touchFilestoreFile("000", "test1.data");      // created
                sleep(1000);
                demoFS.appendToFilestoreFile("000", "test1.data");   // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("000", "test1.data");     // deleted
                sleep(1000);


                demoFS.createFilestoreFile("001", "test2.data");    // modified
                sleep(1000);
                demoFS.appendToFilestoreFile("001", "test2.data");  // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("001", "test2.data");    // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3000);
            }

            // wait to receive the termination event
            sleep(1000);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }

    @Test 
    @EnableOnMac
    void testFileWatcherSubDir_DynamicallyAdded() {
        printf("%n%n[FileWatcherTest::testFileWatcherSubDir_DynamicallyAdded]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

                printf("Ready to watch%n%n");

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!
                demoFS.touchFilestoreFile("000", "test1.data");      // created
                sleep(1000);
                demoFS.appendToFilestoreFile("000", "test1.data");   // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("000", "test1.data");     // deleted
                sleep(1000);

                // a new subdir "002" arrives
                demoFS.createFilestoreSubDir("002");

                demoFS.touchFilestoreFile("002", "test3.data");     // created
                sleep(1000);
                demoFS.appendToFilestoreFile("002", "test3.data");  // modified
                sleep(1000);
                demoFS.deleteFilestoreFile("002", "test3.data");    // deleted
                sleep(1000);

                // wait for all events to be processed before closing the watcher
                sleep(3000);
            }

            // wait to receive the termination event
            sleep(1000);

            // analyze the generated events

            assertEquals(6, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }


    @Test 
    @EnableOnMac
    void testFileWatcherSubDir_DynamicSubDirs() {
        printf("%n%n[FileWatcherTest::testFileWatcherSubDir_DynamicSubDirs]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();
            final Queue<Event> terminations = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            sleep(1000);  // prevent the watcher from picking the 'fswatch' mainDir event

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

                sleep(1000);

                printf("Ready to watch%n%n");

                final File dir1 = demoFS.createFilestoreSubDir("0002");
                sleep(1000);

                final File dir2 = demoFS.createFilestoreSubDir("0003");
                sleep(1000);
                
                dir1.delete();
                sleep(1000);
                
                dir2.delete();
                sleep(1000);
                
                // wait for all events to be processed before closing the watcher
                sleep(3000);
            }

            // wait to receive the termination event
            sleep(1000);

            // analyze the generated events

            assertEquals(0, files.size());
            assertEquals(0, errors.size());
            assertEquals(1, terminations.size());
        }
    }



}
