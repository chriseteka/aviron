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

import java.util.List;

import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;
import com.github.jlangch.aviron.impl.util.CollectionUtils;


public class DynamicCpuLimitExample2 {

    public static void main(String[] args) {
        try {
            new DynamicCpuLimitExample2().test();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void test() throws Exception {
        final CpuProfile weekday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final CpuProfile weekend = CpuProfile.of(
                                        "weekend",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  60%",
                                            "09:00-17:59 @  40%",
                                            "18:00-21:59 @  60%",
                                            "22:00-23:59 @ 100%"));

        // Use the profiles wor weekdays (Mon-Fri) and weekend (Sat,Sun)
        final List<CpuProfile> profiles = CollectionUtils.toList(
                                            weekday,  // Mon
                                            weekday,  // Tue
                                            weekday,  // Wed
                                            weekday,  // Thu
                                            weekday,  // Fri
                                            weekend,  // Sat
                                            weekend); // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        // Event though the profile has resolution base on minutes the 
        // 'formatProfilesAsTableByHour' function prints the overview table 
        // for simplicity at an hour resolution!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
