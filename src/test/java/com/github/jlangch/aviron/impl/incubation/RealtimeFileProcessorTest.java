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
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            
            final Queue<Event> files = new ConcurrentLinkedQueue<>();

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
                                                        e -> { printf("File Event: %s%n", e.getPath());
                                                               files.offer(e); })
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

                sleep(2000);
            }

            printf("RealtimeFileProcessor terminated%n%n");

            // analyze the generated events
            assertEquals(3, files.size());
        }
    }


    private void printf(final String format, final Object... args) {
        synchronized(lock) {
            System.out.printf(format, args);
        }
    }

    private void sleep(final int millis) {
        try { Thread.sleep(millis); } catch(Exception ex) {}
    }


    private final Object lock = new Object();
}
