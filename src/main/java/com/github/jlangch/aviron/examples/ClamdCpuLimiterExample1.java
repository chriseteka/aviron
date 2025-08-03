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

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.github.jlangch.aviron.util.DemoFilestore;
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
 *   |     +-- 0000
 *   |     |     \_ file1.doc
 *   |     |     \_ file2.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     +-- 0001
 *   |     |     \_ file1.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     :
 *   |     +-- NNNN
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
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.populateWithDemoFiles(5, 10);  // 5 sub dirs, each with 10 files

            // demoFS.createEicarAntiMalwareTestFile("0000");

            final Client client = new Client.Builder()
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
                                                "06:00-08:59 @  50%",
                                                "09:00-17:59 @   0%", // no scans
                                                "18:00-21:59 @  50%",
                                                "22:00-23:59 @ 100%"));

            final String clamdPID = ClamdAdmin.getClamdPID();

            final ClamdCpuLimiter limiter = new ClamdCpuLimiter(new DynamicCpuLimit(everyday));
            limiter.setClamdCpuLimitChangeListener(this::onCpuLimitChangeEvent);

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "0000" ⇨ "0001" ⇨ ... ⇨ "NNNN" ⇨ "0000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();

            // inital CPU limit after startup
            limiter.activateClamdCpuLimit(clamdPID);

            // scan the file store directories in an endless loop until we get 
            // killed or stopped
            while(!stop.get()) {
                // update clamd CPU limit 
                limiter.activateClamdCpuLimit(clamdPID);

                final int limit = limiter.getLastSeenLimit();
                if (limit >= MIN_SCAN_LIMIT_PERCENT) {
                    // scan next file store directory
                    final File dir = fsDirCycler.nextDir();
                    final ScanResult result = client.scan(dir.toPath(), true);
                    
                    printf("%s%n", result);
                }
                else {
                    Thread.sleep(30_000);  // wait 30s
                }
            }
        }
    }

    private void onCpuLimitChangeEvent(final ClamdCpuLimitChangeEvent event) {
        printf("Adjusted clamd CPU limit: %d%% -> %d%%%n", event.getOldLimit(), event.getNewLimit());
    }

    private void onQuarantineEvent(final QuarantineEvent event) {
        if (event.getException() != null) {
            printf("Error %s%n", event.getException().getMessage());
        }
        else {
            printf("File %s moved to quarantine%n", event.getInfectedFile() + "");
        }
    }

    private void printf(final String format, final Object... args) {
        synchronized(lock) {
            System.out.printf(format, args);
        }
    }


    private static final int MIN_SCAN_LIMIT_PERCENT = 20;

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Object lock = new Object();
}
