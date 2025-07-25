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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.admin.ClamdCpuLimiter.DynamicCpuLimit;


class DynamicCpuLimitTest {

    @Test
    void testWithDefaults() {
        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit();

        assertEquals(100, dynamicCpuLimit.computeCpuLimit());

        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  6, 0)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 14,30)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,30)));
    }

    @Test
    void testWithAllDayProfile() {
        final CpuProfile profile = new CpuProfile(
                                        "weekday", 
                                        "00:00-05:59 @ 100%, " +
                                        "06:00-08:59 @ 50%, " +
                                        "09:00-17:59 @ 0%, " +
                                        "18:00-21:59 @ 50%, " +
                                        "22:00-23:59 @ 100%");

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profile);

        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  4,21)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  7,30)));
        assertEquals(  0, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 12,35)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 19,48)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,51)));
    }

    @Test
    void testWithWeekDaysProfile() {
        final CpuProfile weekday = new CpuProfile(
                                        "weekday", 
                                        "00:00-05:59 @ 100%, " +
                                        "06:00-08:59 @ 50%, " +
                                        "09:00-17:59 @ 0%, " +
                                        "18:00-21:59 @ 50%, " +
                                        "22:00-23:59 @ 100%");

        final CpuProfile weekend = new CpuProfile(
                                        "weekday", 
                                        "00:00-05:59 @ 100%, " +
                                        "06:00-08:59 @ 60%, " +
                                        "09:00-17:59 @ 40%, " +
                                        "18:00-21:59 @ 60%, " +
                                        "22:00-23:59 @ 100%");
        
        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(weekday); // Mon
        profiles.add(weekday); // Tue
        profiles.add(weekday); // Wed
        profiles.add(weekday); // Thu
        profiles.add(weekday); // Fri
        profiles.add(weekend); // Sat
        profiles.add(weekend); // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        // weekday (Tuesday 2025-07-01)
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  4,21)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  7,30)));
        assertEquals(  0, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 12,35)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 19,48)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,51)));

        // weekend (Saturday 2025-07-05)
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,5,  4,21)));
        assertEquals( 60, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,5,  7,30)));
        assertEquals( 40, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,5, 12,35)));
        assertEquals( 60, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,5, 19,48)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,5, 23,51)));
    }

    @Test
    void testWithComputeFunction1() {
        final Function<LocalDateTime,Integer> computeLimitFn = (ts) -> 80;

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(computeLimitFn);

        assertEquals(80, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  4,21)));
        assertEquals(80, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  7,30)));
        assertEquals(80, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 12,35)));
        assertEquals(80, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 19,48)));
        assertEquals(80, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,51)));
    }

    @Test
    void testWithComputeFunction2() {
        final Function<LocalDateTime,Integer> computeLimitFn = (ts) -> ts.getHour() % 2 == 0 ? 100 : 20;

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(computeLimitFn);

        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  4,21)));
        assertEquals( 20, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  7,30)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 12,35)));
        assertEquals( 20, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 19,48)));
        assertEquals( 20, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,51)));
    }

}
