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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.events.RealtimeScanEvent;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEventType;
import com.github.jlangch.aviron.impl.util.OS;
import com.github.jlangch.aviron.util.DemoFilestore;


// +--------------------------------------------------------------------------+
// |                                                                          |
// |                NOT YET TESTED    -->   DO NOT USE                        |
// |                                                                          |
// +--------------------------------------------------------------------------+

public class RealtimeScannerExample {

    public static void main(String[] args) {
        try {
            new RealtimeScannerExample().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.populateWithDemoFiles(5, 10);  // 5 sub dirs, each with 10 files

            final Client client = new Client.Builder()
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(this::onQuarantineEvent)
                                            .build();

            final Path mainDir = demoFS.getFilestoreDir().toPath();
            final boolean registerAllSubDirs = true;

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, registerAllSubDirs);

            
            final int sleepTimeSecondsOnIdle = 5;
            final boolean testMode = true; // skip file scans with clamd in test mode

            try (RealtimeScanner rtScanner = new RealtimeScanner(
                                                    client, 
                                                    fw,
                                                    sleepTimeSecondsOnIdle,
                                                    testMode,
                                                    this::scanApprover,
                                                    this::onScan)) {
                rtScanner.start();

                Thread.sleep(1000);
                
                demoFS.createFilestoreFile("0000", "test1.data");

                Thread.sleep(1000);

                // demoFS.createEicarAntiMalwareTestFile("0000");

                while(!stop.get()) {
                    Thread.sleep(1000);
                }
            }
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

    private boolean scanApprover(final FileWatchFileEvent event) {
        final String filename = event.getPath().toFile().getName();

        return event.getType() == FileWatchFileEventType.CREATED
                && filename.matches(".*[.](docx|xlsx|pdf)");
    }

    private void onScan(final RealtimeScanEvent event) {
        if (event.hasVirus()) {
            printf("Infected %s%n", event.getPath());
        }
    }

    private void onQuarantineEvent(final QuarantineEvent event) {
        if (event.getException() != null) {
            printf("Error %s%n", event.getException().getMessage());
        }
        else {
            printf("File %s moved to quarantine!%n", event.getInfectedFile());
        }
    }

    private void printf(final String format, final Object... args) {
        synchronized(lock) {
            System.out.printf(format, args);
        }
    }


    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final Object lock = new Object();
}
