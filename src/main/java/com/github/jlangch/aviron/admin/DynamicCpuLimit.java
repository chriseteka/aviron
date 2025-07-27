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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class DynamicCpuLimit {

    public DynamicCpuLimit() {
        this((CpuProfile)null);
    }

    public DynamicCpuLimit(final CpuProfile profile) {
        final CpuProfile p = profile != null ? profile : CpuProfile.defaultProfile();
        this.dynamicLimitFn = (ts) -> p.getLimit(ts.toLocalTime());
        this.dayOfWeekProfiles.add(profile);
    }

    public DynamicCpuLimit(final List<CpuProfile> dayOfWeekProfiles) {
        if (dayOfWeekProfiles == null) {
            throw new IllegalArgumentException(
                    "A dayOfWeekProfiles list must not be null");
        }
        if (dayOfWeekProfiles.size() != 7) {
            throw new IllegalArgumentException(
                    "A dayOfWeekProfiles list must provide 7 entries (Mon to Sun)");
        }
        if (dayOfWeekProfiles.stream().anyMatch(p -> p == null)) {
            throw new IllegalArgumentException(
                    "At least one item of the dayOfWeekProfiles list is null!");
        }

        final List<CpuProfile> profiles = new ArrayList<>(dayOfWeekProfiles);

        this.dynamicLimitFn = (ts) -> { final int dayOfWeek = ts.getDayOfWeek().getValue();
                                        return profiles
                                                  .get(dayOfWeek-1)
                                                  .getLimit(ts.toLocalTime()); };
        this.dayOfWeekProfiles.addAll(dayOfWeekProfiles);
    }

    public DynamicCpuLimit(final Function<LocalDateTime,Integer> dynamicLimitFn) {
        if (dynamicLimitFn == null) {
            throw new IllegalArgumentException(
                    "A dynamicLimitFn function must not be null");
        }

        this.dynamicLimitFn = dynamicLimitFn;
    }


    public int computeCpuLimit() {
        return computeCpuLimit(LocalDateTime.now());
    }

    public int computeCpuLimit(final LocalDateTime ts) {
        if (ts == null) {
            throw new IllegalArgumentException("A ts must not be null");
        }
        
        final int limit = dynamicLimitFn.apply(ts);
        return Math.max(0,  limit);
    }

    public String formatProfilesAsTableByHour() {
        final StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------------------");
        sb.append(System.lineSeparator());
        sb.append("Time        Mon    Tue    Wed    Thu    Fri    Sat    Sun");
        sb.append(System.lineSeparator());
        sb.append("---------------------------------------------------------");
        sb.append(System.lineSeparator());

        for(int hour=0; hour<24; hour++) {
            sb.append(
                String.format(
                    "%02d:00     %4d%%  %4d%%  %4d%%  %4d%%  %4d%%  %4d%%  %4d%%",
                    hour,
                    computeCpuLimit(LocalDateTime.of(2025,7,21,hour,0,0)),    /* Mon */
                    computeCpuLimit(LocalDateTime.of(2025,7,22,hour,0,0)),    /* Tue */
                    computeCpuLimit(LocalDateTime.of(2025,7,23,hour,0,0)),    /* Wed */
                    computeCpuLimit(LocalDateTime.of(2025,7,24,hour,0,0)),    /* Thu */
                    computeCpuLimit(LocalDateTime.of(2025,7,25,hour,0,0)),    /* Fri */
                    computeCpuLimit(LocalDateTime.of(2025,7,26,hour,0,0)),    /* Sat */
                    computeCpuLimit(LocalDateTime.of(2025,7,27,hour,0,0))));  /* Sun */
            sb.append(System.lineSeparator());
        }

        sb.append("---------------------------------------------------------");
        sb.append(System.lineSeparator());

        return sb.toString();
    };


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        if (dayOfWeekProfiles.isEmpty()) {
            sb.append("Compute Function (fn[LocalDateTime] -> int)");
        }
        else if (dayOfWeekProfiles.size() == 1) {
            sb.append("Single day profile:");
            sb.append(System.lineSeparator());
            final CpuProfile p = dayOfWeekProfiles.get(0);
            p.getEntries().forEach(e -> {
                sb.append("  " + e.toString());
                sb.append(System.lineSeparator());
            });
        }
        else {
            sb.append("7 day profile:");
            sb.append(System.lineSeparator());
            int dayOfWeek = 0;
            for(CpuProfile p : dayOfWeekProfiles) {
                sb.append("  " + DAYS[dayOfWeek]);
                sb.append(System.lineSeparator());
                p.getEntries().forEach(e -> {
                    sb.append("    " + e.toString());
                    sb.append(System.lineSeparator());
                });
                dayOfWeek++;
            }
        }

        return sb.toString();
    }


    private static String[] DAYS = new String[] {
                                        "Monday", "Tuesday", "Wednesday", "Thursday", 
                                        "Friday", "Saturday", "Sunday"};

    private final Function<LocalDateTime,Integer> dynamicLimitFn;
    private final List<CpuProfile> dayOfWeekProfiles = new ArrayList<>();
}
