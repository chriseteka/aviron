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
package com.github.jlangch.aviron.examples;

import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;
import static com.github.jlangch.aviron.util.Util.printfln;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.ClamdCpuLimitChangeEvent;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.limiter.ClamdCpuLimiter;
import com.github.jlangch.aviron.limiter.ClamdPid;
import com.github.jlangch.aviron.limiter.CpuProfile;
import com.github.jlangch.aviron.limiter.DynamicCpuLimit;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.DirCycler;
import com.github.jlangch.aviron.util.IDirCycler;


/**
 * Clamd CpuLimiter Example 1
 * 
 * 
 * The demo filestore layout:
 * 
 * <pre>
 * demo/
 *   |
 *   +-- filestore/
 *   |     |
 *   |     +-- 000
 *   |     |     \_ file1.doc
 *   |     |     \_ file2.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     +-- 001
 *   |     |     \_ file1.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     :
 *   |     +-- NNN
 *   |           \_ file1.doc
 *   |
 *   +-- quarantine/
 *         \_ eicar.txt
 *         \_ eicar.txt.virus
 * </pre>
 */
public class ClamdCpuLimiterExample1 {

    public static void main(String[] args) {
        try {
            new ClamdCpuLimiterExample1().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
        printfln("Starting %s...", MOCKING ? "in MOCKING mode " : "");

        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.populateWithDemoFiles(5, 10);  // 5 sub dirs, each with 10 files

            // demoFS.createEicarAntiMalwareTestFile("000");

            final Client client = new Client.Builder()
                                            .mocking(MOCKING)  // turn mocking on/off
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(this::onQuarantineEvent)
                                            .build();

            // Use the same day profile for Mon - Sun
            final CpuProfile everyday = CpuProfile.of(
                                            "weekday",
                                            toList(
                                                "00:00-05:59 @ 100%",
                                                "06:00-07:59 @   0%", // no scans
                                                "08:00-17:59 @  50%",
                                                "18:00-21:59 @  50%",
                                                "22:00-23:59 @ 100%"));

            // replace the demo clamd pid file with your real one or pass clamd PID
            final ClamdPid clamdPID = new ClamdPid(demoFS.getClamdPidFile());

            final ClamdCpuLimiter limiter = new ClamdCpuLimiter(clamdPID, new DynamicCpuLimit(everyday));
            limiter.setClamdCpuLimitChangeListener(this::onCpuLimitChangeEvent);
            limiter.mocking(MOCKING); // turn mocking on/off

            // create a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = new DirCycler(demoFS.getFilestoreDir());

            // initial CPU limit after startup
            limiter.activateClamdCpuLimit();

            printfln("Processing ...");

            // scan the file store directories in an endless loop until we get 
            // killed or stopped
            while(!stop.get()) {
                // explicitly update clamd CPU limit 
                limiter.activateClamdCpuLimit();

                final int limit = limiter.getLastSeenLimit();
                if (limit >= MIN_SCAN_LIMIT_PERCENT) {
                    // scan next file store directory
                    final File dir = fsDirCycler.nextDir();

                    if (MOCKING) {
                        printfln("Simulated dir scan: %s", dir.toPath());
                        Thread.sleep(10_000);
                    }
                    else {
                        final ScanResult result = client.scan(dir.toPath(), true);
                        printfln("Scanned dir %s: %s", dir.toPath(), result);
                    }
                }
                else {
                    // pause 30s due to temporarily suspended scanning (by CpuProfile)
                    printfln("Scanning currently paused by CPU profile");
                    Thread.sleep(30_000);
                }
            }

            printfln("Stopped");
        }
    }

    private void onCpuLimitChangeEvent(final ClamdCpuLimitChangeEvent event) {
        printfln("Adjusted %s", event);
    }

    private void onQuarantineEvent(final QuarantineEvent event) {
        if (event.getException() != null) {
            printfln("Error %s", event.getException().getMessage());
        }
        else {
            printfln("File %s moved to quarantine", event.getInfectedFile());
        }
    }


    // mocking turned on for demo
    private static final boolean MOCKING = true;
    // below this cpu percentage file scanning is paused
    private static final int MIN_SCAN_LIMIT_PERCENT = 20;

    private final AtomicBoolean stop = new AtomicBoolean(false);
}
