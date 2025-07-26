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


    private final Function<LocalDateTime,Integer> dynamicLimitFn;
}
