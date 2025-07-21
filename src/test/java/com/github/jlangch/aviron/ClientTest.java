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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;


class ClientTest {

    @Test void test() {
        // without clamd mock the client must be mainly manually tested
    }

    @Test void testCreate1() {
        final Client client = Client.builder()
                                    .serverHostname("localhost")
                                    .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                    .build();
        assertNotNull(client);
    }

    @Test void testCreate2() throws IOException {
        final File quanrantineDir = Files.createTempDirectory("quarantine_").toFile();
        quanrantineDir.deleteOnExit();
        
        final Client client = Client.builder()
                                    .serverHostname("localhost")
                                    .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                    .quarantineDir(quanrantineDir)
                                    .quarantineFileAction(QuarantineFileAction.NONE)
                                    .quarantineEventListener(this::listener)
                                    .build();
        assertNotNull(client);
    }


    private void listener(final QuarantineEvent event) {
        
    }
}
