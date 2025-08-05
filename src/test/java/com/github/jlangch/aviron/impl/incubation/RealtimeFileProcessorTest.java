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

import static com.github.jlangch.aviron.util.Util.printf;
import static com.github.jlangch.aviron.util.Util.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.events.Event;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.junit.EnableOnMac;


public class RealtimeFileProcessorTest {

    @Test 
    @EnableOnMac
    void testRealtimeFileProcessor() {
        printf("%n%n[RealtimeFileProcessorTest::testRealtimeFileProcessor]%n%n");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Queue<Event> files = new ConcurrentLinkedQueue<>();
            final Queue<Event> errors = new ConcurrentLinkedQueue<>();

            final Path mainDir = demoFS.getFilestoreDir().toPath();
            
            final IFileWatcher fw = new FileWatcher_FsWatch(
                                            mainDir, 
                                            true,  
                                            null, // default fswatch monitor
                                            FileWatcher_FsWatch.HOMEBREW_FSWATCH_PROGRAM);

            // start the realtime file processor and process the incoming
            // file events in the onScan() listener
            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        0,
                                                        e -> { printf("RT File Event: %s%n", e.getPath());
                                                               files.offer(e); },
                                                        e -> { printf("File Watch Error: %s %s%n", 
                                                                      e.getPath(), 
                                                                      e.getException().getMessage());
                                                               errors.offer(e); })
            ) {
                rtScanner.start();

                printf("RealtimeFileProcessor started%n");

                sleep(200);

                demoFS.createFilestoreFile("000", "test1.data");
                demoFS.createFilestoreFile("000", "test2.data");
                demoFS.createFilestoreFile("000", "test3.data");

                sleep(500);
                demoFS.createFilestoreSubDir("002");
                demoFS.createFilestoreFile("002", "test4.data");

                sleep(500);
                demoFS.createFilestoreFile("002", "test5.data");
                // this should discard the create for "test5.data" 
                // unless the delete event "test5.data" arrives too late
                demoFS.deleteFilestoreFile("002", "test5.data");

                sleep(2000);
            }

            printf("RealtimeFileProcessor terminated%n%n");

            // analyze the generated events
            assertEquals(4, files.size());
            assertEquals(0, errors.size());
        }
    }

}
