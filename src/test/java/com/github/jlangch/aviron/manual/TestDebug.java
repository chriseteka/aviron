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
package com.github.jlangch.aviron.manual;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;


public class TestDebug {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();

        System.out.println("[Stats]");
        System.out.println(client.stats());
        System.out.println();
        System.out.println();

        System.out.println("[Last Command]");
        System.out.println(client.lastCommandRunDetails());
        System.out.println();
        System.out.println();
    }

}
