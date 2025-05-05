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

import com.github.jlangch.aviron.admin.Admin;
import com.github.jlangch.aviron.util.ShellBackgroundResult;


public class TestAdminCpulimit {

    /* 
     * Update virus database:       freshclam
     * Start clamd in background:   clamd   (just wait 10s :-))
     */
    public static void main(String[] args) throws Exception {

        if (Admin.getClamdPID() == null) {
            System.out.println("clamd is not running");
            return;
        }

        System.out.println("[clamd PID]");
        System.out.println(Admin.getClamdPID());
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(Admin.getCpulimitPIDs());
        System.out.println();
        System.out.println();

        System.out.println("[activate cpulimit]");
        final ShellBackgroundResult bgResult = Admin.activateCpuLimit(50);
        Thread.sleep(5000);
        System.out.println(bgResult);
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(Admin.getCpulimitPIDs());
        System.out.println();
        System.out.println();

        System.out.println("[kill clamd]");
        if (Admin.getClamdPID() != null) {
            Admin.killClamd();
            Thread.sleep(3000);
            System.out.println("killed");
            System.out.println();
            System.out.println();
        }
        else {
            System.out.println("not running -> skipped");
            System.out.println();
            System.out.println();
        }

        System.out.println("[clamd PID]");
        System.out.println(Admin.getClamdPID());
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(Admin.getCpulimitPIDs());
        System.out.println();
        System.out.println();
    }

}
