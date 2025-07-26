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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Defines a 24 hours CPU limit profile
 */
public class CpuProfile {

    /**
     * Create a named CPU profile
     * 
     * <pre>
     * final List&lt;CpuProfileEntry&gt; entries = new ArrayList&lt;&gt;();
     * entries.add(Entry.parse("00:00-05:59 @ 100%"));
     * entries.add(Entry.parse("06:00-08:59 @ 50%"));
     * entries.add(Entry.parse("09:00-17:59 @ 0%"));
     * entries.add(Entry.parse("18:00-21:59 @ 50%"));
     * entries.add(Entry.parse("22:00-23:59 @ 100%"));
     * 
     * final CpuProfile profile = new CpuProfile("weekday", entries);
     * </pre>
     * 
     * @param name the profil's name
     * @param entries the profile's entries
     */
    public CpuProfile(final String name, final List<CpuProfileEntry> entries) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("A name time must not be blank!");
        }
        if (entries == null) {
            throw new IllegalArgumentException("An entries list must not be null!");
        }

        validate(entries);

        this.name = name;
        this.entries.addAll(entries);
    }

    /**
     * Create a named CPU profile
     * 
     * <pre>
     * final CpuProfile profile = new CpuProfile(
     *                                  "weekday", 
     *                                  "00:00-05:59 @ 100%, " +
     *                                  "06:00-08:59 @ 50%,  " +
     *                                  "09:00-17:59 @ 0%,   " +
     *                                  "18:00-21:59 @ 50%,  " +
     *                                  "22:00-23:59 @ 100%");
     * </pre>
     * 
     * @param name the profil's name
     * @param entries the formatted profile's entries (a string with comma 
     *        separated list of stringified profile entries)
     */
    public CpuProfile(final String name, final String entries) {
        this(name, Arrays
                      .stream(entries.split(" *, *"))
                      .map(e -> CpuProfileEntry.parse(e))
                      .collect(Collectors.toList()));
    }

    /**
     * Returns the default profile: "00:00-23:59 @ 100%"
     * 
     * @return the default profile
     */
    public static CpuProfile defaultProfile() {
        return new CpuProfile("default","00:00-23:59 @ 100%");
    }

    public String getName() {
        return name;
    }

    public List<CpuProfileEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getLimit(final int hour, final int minute) {
        return getLimit(LocalTime.of(hour, minute));
    }

    public int getLimit(final LocalTime time) {
        if (time == null) {
            return off.getLimit();
        }

        return entries
                .stream()
                .filter(e -> e.isWithin(time))
                .findFirst()
                .orElse(off)
                .getLimit();
    }


    private void validate(final List<CpuProfileEntry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("An entries list must not be empty!");
        }
        else if (entries.size() == 1) {
            return;
        }
        else {
            for(int ii=0; ii<entries.size()-1; ii++) {
                if (!entries.get(ii).isBefore(entries.get(ii+1))) {
                    throw new IllegalArgumentException(
                            "The entries are not in ascending order or are overlapping! "
                            + "Check entry: \"" + entries.get(ii+1) + "\"");
                }
            }
        }
    }

    
    private final static CpuProfileEntry off = new CpuProfileEntry(
    													LocalTime.of(0, 0), 
    													LocalTime.of(23, 59), 
    													0);

    private final String name;
    private final List<CpuProfileEntry> entries = new ArrayList<>();
}
