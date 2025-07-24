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

import com.github.jlangch.aviron.ClamdAdmin;
import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;


public class TestShutdownServer {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();
        
        System.out.println("[Shutdown Server]");
        client.shutdownServer();
        System.out.println("Shutdown.");
        System.out.println();
        System.out.println();

        Thread.sleep(1000);

        System.out.println("[Reachable]");
        System.out.println(client.isReachable());
        System.out.println();
        System.out.println();

        System.out.println("[Reachable (Timeout)]");
        System.out.println(client.isReachable(2000));
        System.out.println();
        System.out.println();

        System.out.println("[clamd PID]");
        System.out.println(ClamdAdmin.getClamdPID());
        System.out.println();
        System.out.println();

    }

}
