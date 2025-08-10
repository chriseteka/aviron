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
package com.github.jlangch.aviron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.processor.RealtimeFileProcessor;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.IDirCycler;
import com.github.jlangch.aviron.util.OS;
import com.github.jlangch.aviron.util.Util;
import com.github.jlangch.aviron.util.junit.EnableOnMac;


class ScannerTest {

    @Test 
    @EnableOnMac
    public void testFilestoreBackgroundScan() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();

            demoFS.populateWithDemoFiles(3, 5);  // 3 sub dirs, each with 5 files

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();
            
            // scan the file store directories in an endless loop until we get 
            // stopped
            final long stopAt = System.currentTimeMillis() + 5_000; // run 5s
            while(System.currentTimeMillis() < stopAt) {
                // scan next file store directory
                final File dir = fsDirCycler.nextDir();
                
                scan.accept(dir.toPath());
 
                Util.sleep(200);
             }

            assertTrue(scanOK.get() > 0);
            assertEquals(0, scanVirus.get());
            assertEquals(0, quarantine.get());
        }
    }


    @Test 
    @EnableOnMac
    public void testFilestoreRealtimeScan() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();
            final AtomicLong errors = new AtomicLong();

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, true);

            // start the realtime file processor and process the incoming
            // file events in the scan listener
            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        1,
                                                        e -> scan.accept(e.getPath()),
                                                        e -> errors.incrementAndGet())
            ) {
                rtScanner.start();

                Util.sleep(1_000);

                demoFS.createFilestoreFile("000", "test-rt-1.data");
                
                // let the file watcher work

                Util.sleep(4_000);
            }

            assertEquals(1, scanOK.get());
            assertEquals(0, errors.get());
            assertEquals(0, scanVirus.get());
            assertEquals(0, quarantine.get());
        }
    }


    @Test 
    @EnableOnMac
    public void testFilestoreBackgroundAndRealtimeScan() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();
            final AtomicLong errors = new AtomicLong();

            demoFS.populateWithDemoFiles(3, 5);  // 3 sub dirs, each with 5 files

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, true);

            // start the realtime file processor and process the incoming
            // file events in the scan listener
            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        1,
                                                        e -> scan.accept(e.getPath()),
                                                        e -> errors.incrementAndGet())
            ) {
                rtScanner.start();

                Util.sleep(1_000);

                demoFS.createFilestoreFile("000", "test-rt-1.data");
                
                // background scanner: scan the file store directories in an 
                //                     endless loop until we get  stopped
                final long stopAt = System.currentTimeMillis() + 4_000; // run 4s
                while(System.currentTimeMillis() < stopAt) {
                    // scan next file store directory
                    final File dir = fsDirCycler.nextDir();
                    
                    scan.accept(dir.toPath());
                    
                    Util.sleep(200);
                 }
            }

            assertTrue(scanOK.get() > 0);
            assertEquals(0, errors.get());
            assertEquals(0, scanVirus.get());
            assertEquals(0, quarantine.get());
        }
    }


    @Test 
    @EnableOnMac
    public void testFilestoreBackgroundScanWithVirus() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();

            demoFS.populateWithDemoFiles(3, 5);  // 3 sub dirs, each with 5 files

            final File eicarFile = demoFS.createEicarAntiMalwareTestFile("000");

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();

            // test eicar file
            final ScanResult eicarResult = client.scan(eicarFile.toPath(), true);
            if (eicarResult.isOK()) {
                scanOK.incrementAndGet();
            }
            else {
                scanVirus.incrementAndGet();
            }

            // scan the file store directories in an endless loop until we get 
            // stopped
            final long stopAt = System.currentTimeMillis() + 5_000; // run 5s
            while(System.currentTimeMillis() < stopAt) {
                // scan next file store directory
                final File dir = fsDirCycler.nextDir();
                
                scan.accept(dir.toPath());
                
                Util.sleep(200);
             }

            assertTrue(scanOK.get() > 0);
            assertEquals(1, scanVirus.get());
            assertEquals(1, quarantine.get());     
            assertEquals(1, demoFS.countQuarantineFiles());
        }
    }


    @Test 
    @EnableOnMac
    public void testFilestoreRealtimeScanWithVirus() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();
            final AtomicLong errors = new AtomicLong();
            
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, true);

            // start the realtime file processor and process the incoming
            // file events in the scan listener
            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        1,
                                                        e -> scan.accept(e.getPath()),
                                                        e -> errors.incrementAndGet())
            ) {
                rtScanner.start();

                Util.sleep(1_000);

                demoFS.createFilestoreFile("000", "test-rt-1.data");
                
                demoFS.createEicarAntiMalwareTestFile("000");
                
                // let the file watcher work

                Util.sleep(4_000);
            }

            assertEquals(1, scanOK.get());
            assertEquals(0, errors.get());
            assertEquals(1, scanVirus.get());
            assertEquals(1, quarantine.get());
        }
    }


    @Test 
    @EnableOnMac
    public void testFilestoreBackgroundAndRealtimeScanWithVirus() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final AtomicLong scanOK = new AtomicLong();
            final AtomicLong scanVirus = new AtomicLong();
            final AtomicLong quarantine = new AtomicLong();
            final AtomicLong errors = new AtomicLong();
            
            demoFS.populateWithDemoFiles(3, 5);  // 3 sub dirs, each with 5 files

            final Client client = new Client.Builder()
                                            .mocking(true)
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(e -> quarantine.incrementAndGet())
                                            .build();
            
            final Consumer<Path> scan = (path) -> { final ScanResult result = client.scan(path);
                                                    if (result.isOK()) {
                                                        scanOK.incrementAndGet();
                                                    }
                                                    else {
                                                        scanVirus.incrementAndGet();
                                                    } };

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();

            final Path mainDir = demoFS.getFilestoreDir().toPath();

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, true);

            // start the realtime file processor and process the incoming
            // file events in the scan listener
            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        1,
                                                        e -> scan.accept(e.getPath()),
                                                        e -> errors.incrementAndGet())
            ) {
                rtScanner.start();

                Util.sleep(1_000);

                demoFS.createFilestoreFile("000", "test-rt-1.data");

                demoFS.createEicarAntiMalwareTestFile("000");

                // background scanner: scan the file store directories in an 
                //                     endless loop until we get  stopped
                final long stopAt = System.currentTimeMillis() + 4_000; // run 4s
                while(System.currentTimeMillis() < stopAt) {
                    // scan next file store directory
                    final File dir = fsDirCycler.nextDir();

                    scan.accept(dir.toPath());
                    
                    Util.sleep(200);
                 }
            }
 
            assertTrue(scanOK.get() > 0);
            assertEquals(0, errors.get());
            assertEquals(1, scanVirus.get());
            assertEquals(1, quarantine.get());
        }
    }



    private IFileWatcher createPlatformFileWatcher(
            final Path mainDir, 
            final boolean registerAllSubDirs
    ) {
        if (OS.isLinux()) {
            return new FileWatcher_JavaWatchService(mainDir, registerAllSubDirs);
        }
        else if (OS.isMacOSX()) {
            return new FileWatcher_FsWatch(
                         mainDir,
                         registerAllSubDirs,
                         null, // default fswatch monitor
                         FileWatcher_FsWatch.HOMEBREW_FSWATCH_PROGRAM);
        }
        else {
            throw new FileWatcherException(
                    "FileWatcher is not supported on platforms other than Linux/MacOS!");
        }
    }

}
