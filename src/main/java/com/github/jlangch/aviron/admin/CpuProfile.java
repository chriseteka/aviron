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
package com.github.jlangch.aviron.admin;


/**
 * Defines a 24 hours CPU profile
 */
public class CpuProfile {
    
    public CpuProfile(final String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    // TODO: implement

    /*
    
                   time range     cpu
                   -------------  ---
     { :weekday  [["00:00-02:45"  100]
                  ["02:45-04:30"    0]
                  ["04:30-06:00"  100]
                  ["06:00-07:00"   70]
                  ["07:00-08:00"   30]
                  ["08:00-18:00"    0]
                  ["18:00-20:00"   30]
                  ["20:00-21:00"   40]
                  ["21:00-22:00"   50]
                  ["22:00-24:00"  100]]

       :weekend  [["00:00-02:45"  100]
                  ["02:45-04:30"    0]
                  ["04:30-08:00"  100]
                  ["08:00-09:00"   75]
                  ["09:00-10:00"   50]
                  ["10:00-22:00"   30]
                  ["22:00-23:00"   50]
                  ["23:00-24:00"   75]] }

     */


    private final String name;
}
