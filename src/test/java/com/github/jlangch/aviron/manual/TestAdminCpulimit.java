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

import static com.github.jlangch.aviron.impl.util.CollectionUtils.first;

import com.github.jlangch.aviron.limiter.ClamdPid;


public class TestAdminCpulimit {

    /* 
     * Update virus database:       freshclam
     * Start clamd in background:   clamd   (just wait 10s :-))
     */
    public static void main(String[] args) throws Exception {

        if (ClamdPid.getPids().isEmpty()) {
            System.out.println("clamd is not running");
            return;
        }

        System.out.println("[clamd PID]");
        System.out.println(first(ClamdPid.getPids()));
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(ClamdPid.getCpulimitPids());
        System.out.println();
        System.out.println();

        System.out.println("[activate cpulimit]");
        new ClamdPid(first(ClamdPid.getPids())).activateCpuLimit(50);
        Thread.sleep(5000);
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(ClamdPid.getPids());
        System.out.println();
        System.out.println();

        System.out.println("[kill clamd]");
        final String pid = first(ClamdPid.getPids());
        if (pid!= null) {
            new ClamdPid(pid).kill();
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
        System.out.println(first(ClamdPid.getPids()));
        System.out.println();
        System.out.println();

        System.out.println("[cpulimit PIDs]");
        System.out.println(ClamdPid.getCpulimitPids());
        System.out.println();
        System.out.println();
    }

}
