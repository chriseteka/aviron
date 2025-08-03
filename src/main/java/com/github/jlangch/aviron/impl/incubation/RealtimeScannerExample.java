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

import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.admin.ClamdAdmin;
import com.github.jlangch.aviron.admin.ClamdCpuLimiter;
import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.ClamdCpuLimitChangeEvent;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.events.RealtimeScanEvent;
import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.OS;


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

            client.set(new Client.Builder()
                                 .serverHostname("localhost")
                                 .serverFileSeparator(FileSeparator.UNIX)
                                 .quarantineFileAction(QuarantineFileAction.MOVE)
                                 .quarantineDir(demoFS.getQuarantineDir())
                                 .quarantineEventListener(this::onQuarantineEvent)
                                 .build());

            // Use the same day profile for Mon - Sun
            final CpuProfile everyday = CpuProfile.of(
                                            "weekday",
                                            toList(
                                                "00:00-05:59 @ 100%",
                                                "06:00-08:59 @  50%",
                                                "09:00-17:59 @  50%",
                                                "18:00-21:59 @  50%",
                                                "22:00-23:59 @ 100%"));

            limiter.set(new ClamdCpuLimiter(
                                new DynamicCpuLimit(everyday),
                                this::onCpuLimitChangeEvent));

            final String pid = ClamdAdmin.getClamdPID();
            clamdPID.set(pid);

            final Path mainDir = demoFS.getFilestoreDir().toPath();
            final boolean registerAllSubDirs = true;

            final IFileWatcher fw = createPlatformFileWatcher(mainDir, registerAllSubDirs);

            final int sleepTimeSecondsOnIdle = 5;

            // inital CPU limit after startup
            limiter.get().activateClamdCpuLimit(pid);

            try (RealtimeFileProcessor rtScanner = new RealtimeFileProcessor(
                                                        fw,
                                                        sleepTimeSecondsOnIdle,
                                                        this::onScan)
            ) {
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

    private void onCpuLimitChangeEvent(final ClamdCpuLimitChangeEvent event) {
        printf("Adjusted clamd CPU limit: %d%% -> %d%%%n", event.getOldLimit(), event.getNewLimit());
    }

    private void onScan(final RealtimeScanEvent event) {
        final ClamdCpuLimiter l = limiter.get();

        // update clamd CPU limit 
        l.activateClamdCpuLimit(clamdPID.get());

        final int limit = l.getLastSeenLimit();
        if (limit >= MIN_SCAN_LIMIT_PERCENT) {
            final ScanResult result = client.get().scan(event.getPath(), true);
            printf("%s%n", result);
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


    private static final int MIN_SCAN_LIMIT_PERCENT = 20;

    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final AtomicReference<Client> client = new AtomicReference<>();
    private final AtomicReference<ClamdCpuLimiter> limiter = new AtomicReference<>();
    private final AtomicReference<String> clamdPID = new AtomicReference<>();

    private final Object lock = new Object();
}
