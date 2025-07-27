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

import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;


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
        final CpuProfile profile = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profile);

        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  4,21)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1,  7,30)));
        assertEquals(  0, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 12,35)));
        assertEquals( 50, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 19,48)));
        assertEquals(100, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,1, 23,51)));
    }

    @Test
    void testWithWeekDaysProfile() {
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
    void testWeekday_Weekend() {
        final CpuProfile profile1 = CpuProfile.of("weekday", toList("00:00-23:59 @ 30%"));
        final CpuProfile profile2 = CpuProfile.of("weekend", toList("00:00-23:59 @ 60%"));

        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(profile1);  // Mon
        profiles.add(profile1);  // Tue
        profiles.add(profile1);  // Wed
        profiles.add(profile1);  // Thu
        profiles.add(profile1);  // Fri
        profiles.add(profile2);  // Sat
        profiles.add(profile2);  // Sun
        
        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        assertEquals(30, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,21,12,0)));  // Mon
        assertEquals(30, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,22,12,0)));  // Tue
        assertEquals(30, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,23,12,0)));  // Wed
        assertEquals(30, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,24,12,0)));  // Thu
        assertEquals(30, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,25,12,0)));  // Fri
        assertEquals(60, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,26,12,0)));  // Sat
        assertEquals(60, dynamicCpuLimit.computeCpuLimit(LocalDateTime.of(2025,7,27,12,0)));  // Sun
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

    @Test
    void testToString1() {
        final CpuProfile weekday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(weekday);  // Mon
        profiles.add(weekday);  // Tue
        profiles.add(weekday);  // Wed
        profiles.add(weekday);  // Thu
        profiles.add(weekday);  // Fri
        profiles.add(weekday);  // Sat
        profiles.add(weekday);  // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        final String s = dynamicCpuLimit.toString();
        // System.out.println(s);
        assertNotNull(s);
    }

    @Test
    void testToString2() {
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

        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(weekday);  // Mon
        profiles.add(weekday);  // Tue
        profiles.add(weekday);  // Wed
        profiles.add(weekday);  // Thu
        profiles.add(weekday);  // Fri
        profiles.add(weekend);  // Sat
        profiles.add(weekend);  // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        final String s = dynamicCpuLimit.toString();
        // System.out.println(s);
        assertNotNull(s);
    }

    @Test
    void testFormatProfilesAsTableByHour1() {
        final CpuProfile weekday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(weekday);  // Mon
        profiles.add(weekday);  // Tue
        profiles.add(weekday);  // Wed
        profiles.add(weekday);  // Thu
        profiles.add(weekday);  // Fri
        profiles.add(weekday);  // Sat
        profiles.add(weekday);  // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        // System.out.println(s);
        assertNotNull(s);
    }

    @Test
    void testFormatProfilesAsTableByHour2() {
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

        final List<CpuProfile> profiles = new ArrayList<>();
        profiles.add(weekday);  // Mon
        profiles.add(weekday);  // Tue
        profiles.add(weekday);  // Wed
        profiles.add(weekday);  // Thu
        profiles.add(weekday);  // Fri
        profiles.add(weekend);  // Sat
        profiles.add(weekend);  // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        // System.out.println(s);
        assertNotNull(s);
    }
}
