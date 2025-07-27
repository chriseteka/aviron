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

import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.events.QuarantineEvent;


public class ScanQuarantineExample {

    public static void main(String[] args) throws Exception {
        try {
            new ScanExample().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
       final String baseDir = "/data/files/";
        final String quarantineDir = "/data/quarantine/";

        // Note: The file separator depends on the server's type (Unix, Windows)
        //       clamd is running on!
        final Client client = new Client.Builder()
                                        .serverHostname("localhost")
                                        .serverFileSeparator(FileSeparator.UNIX)
                                        .quarantineFileAction(QuarantineFileAction.MOVE)
                                        .quarantineDir(quarantineDir)
                                        .quarantineEventListener(this::eventListener)
                                        .build();

        System.out.println("Reachable: " + client.isReachable());

        // Scanning -----------------------------------------------------------

        // scan single file
        System.out.println(client.scan(Paths.get(baseDir, "document.pdf")));

        // scan dir (recursive)
        System.out.println(client.scan(Paths.get(baseDir), true));

        // scan streamed data
        try (InputStream is = new FileInputStream(new File(baseDir, "document.pdf"))) {
            System.out.println(client.scan(is));
        }

        // Quarantine Management ----------------------------------------------

        // list quarantine files
        final List<QuarantineFile> files = client.listQuarantineFiles();
        System.out.println(String.format("%d quarantined files", files.size()));

        // show quarantine file details and remove the file
        if (!files.isEmpty()) {
            final QuarantineFile qf = files.get(0);
            System.out.println(qf);
            client.removeQuarantineFile(qf);
        }

        // remove all quarantine files
        client.removeAllQuarantineFiles();
    }
    
    private void eventListener(final QuarantineEvent event) {
        if (event.getException() != null) {
            System.out.println("Error " + event.getException().getMessage());
        }
        else {
            System.out.println("File " + event.getInfectedFile() + " moved to quarantine");
        }
    }
}
