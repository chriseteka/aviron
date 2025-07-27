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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.admin.ClamdAdmin;
import com.github.jlangch.aviron.admin.ClamdCpuLimiter;
import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;


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
        // Our demo filestore looks like:
        //
        // /data/filestore/
        //   |
        //   +-- 0000
        //   |     \_ file1.doc
        //   |     \_ file2.doc
        //   |     :
        //   |     \_ fileN.doc
        //   +-- 0001
        //   :
        //   +-- NNNN
        //         \_ file1.doc
        //
        final File filestoreDir = new File("/data/filestore/");
        final File quarantineDir = new File("/data/quarantine/");

        final Client client = new Client.Builder()
                                        .serverHostname("localhost")
                                        .serverFileSeparator(FileSeparator.UNIX)
                                        .quarantineFileAction(QuarantineFileAction.MOVE)
                                        .quarantineDir(quarantineDir)
                                        .quarantineEventListener(this::eventListener)
                                        .build();

        // Use the same day profile for Mon - Sun
        final CpuProfile everyday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final String clamdPID = ClamdAdmin.getClamdPID();

        final ClamdCpuLimiter limiter = new ClamdCpuLimiter(new DynamicCpuLimit(everyday));

        // inital CPU limit after startup
        initialCpuLimit(limiter, clamdPID);

        // scan until we're killed
        while(true) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(filestoreDir.toPath())) {
                dirStream.forEach(path -> {
                    // update clamd CPU limit 
                    final int limit = updateCpuLimit(limiter, clamdPID);

                    if (limit >= MIN_SCAN_LIMIT) {
                        // Scan the next filestore directory
                        System.out.println(client.scan(path, false));
                    }
                });
            }
            catch(Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void initialCpuLimit(final ClamdCpuLimiter limiter, final String clamdPID) {
        limiter.activateClamdCpuLimit(clamdPID);
        System.out.println(String.format(
                            "Initial clamd CPU limit: %d%%",
                            limiter.getLastSeenLimit()));
    }

    private int updateCpuLimit(final ClamdCpuLimiter limiter, final String clamdPID) {
        // note: applied only if the new limit differs from the last one
        final int lastSeenLimit = limiter.getLastSeenLimit();
        if (limiter.activateClamdCpuLimit(clamdPID)) {
            final int newLimit = limiter.getLastSeenLimit();
            System.out.println(String.format(
                                "Adjusted clamd CPU limit: %d%% -> %d%%",
                                lastSeenLimit, newLimit));
            return newLimit;
        }
        else {
            return lastSeenLimit;
        }
    }

    private void eventListener(final QuarantineEvent event) {
        if (event.getException() != null) {
            System.out.println("Error " + event.getException().getMessage());
        }
        else {
            System.out.println("File " + event.getInfectedFile() + " moved to quarantine");
        }
    }


    private static final int MIN_SCAN_LIMIT = 20;
}
