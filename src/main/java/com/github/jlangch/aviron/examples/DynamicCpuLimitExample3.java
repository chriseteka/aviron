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

import java.time.LocalDateTime;
import java.util.function.Function;

import com.github.jlangch.aviron.limiter.DynamicCpuLimit;


public class DynamicCpuLimitExample3 {

    public static void main(String[] args) {
        try {
            new DynamicCpuLimitExample3().test();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void test() throws Exception {
        final Function<LocalDateTime,Integer> limitFn = 
                (t) -> { final int hour = t.getHour();
                         final int day = t.getDayOfWeek().getValue();
                         if (hour < 8)   return 100;
                         if (hour >= 20) return 100;
                         else            return day > 5 ? 60 : 30; };

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(limitFn);

        // Even though the profile has a minute resolution the 
        // 'formatProfilesAsTableByHour' function prints the overview
        // table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
