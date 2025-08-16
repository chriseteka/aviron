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
package com.github.jlangch.aviron.util;

import java.util.ArrayList;
import java.util.List;


public class DirCyclerStatistics {

    public DirCyclerStatistics() {
    }

    public DirCyclerStatistics(
            final long cycles,
            final List<Long> lastRoundtripTimes
    ) {
        this.cycles = cycles;
        this.lastRoundtripTimes.addAll(lastRoundtripTimes);
    }

    /**
     * @return the number of processed cycles
     */
    public long getCycles() {
        return cycles;
    }

    /**
     * @return a list of the last round trip (full cycle) times in milliseconds.
     */
    public List<Long> getLastRoundtripTimes() {
        return lastRoundtripTimes;
    }

    /**
     * @return a the last round trip (full cycle) time in milliseconds.
     */
    public Long getLastRoundtripTime() {
        return lastRoundtripTimes.isEmpty()
                ? null
                : lastRoundtripTimes.get(lastRoundtripTimes.size()-1);
    }

    public void incrementCycles() {
        cycles++;
    }

    public void addRoundtripTime(final long milliseconds) {
        lastRoundtripTimes.add(milliseconds);
        while (lastRoundtripTimes.size() > 10) {
            lastRoundtripTimes.remove(0);
        }
    }

    private long cycles;
    private final List<Long> lastRoundtripTimes = new ArrayList<>();
}
