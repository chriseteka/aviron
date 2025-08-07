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
package com.github.jlangch.aviron.limiter;

import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;

import java.time.LocalTime;
import java.util.ArrayList;
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
     * final CpuProfile profile = 
     *        new CpuProfile(
     *               "weekday", 
     *               CollectionUtils.toList(
     *                  CpuProfileEntry.parse("00:00-05:59 @ 100%"),
     *                  CpuProfileEntry.parse("06:00-08:59 @  50%"),
     *                  CpuProfileEntry.parse("09:00-17:59 @   0%"),
     *                  CpuProfileEntry.parse("18:00-21:59 @  50%"),
     *                  CpuProfileEntry.parse("22:00-23:59 @ 100%"));
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
     * final CpuProfile profile = CpuProfile.of(
     *                                  "weekday", 
     *                                  CollectionUtils.toList(
     *                                      "00:00-05:59 @ 100%",
     *                                      "06:00-08:59 @  50%",
     *                                      "09:00-17:59 @   0%",
     *                                      "18:00-21:59 @  50%",
     *                                      "22:00-23:59 @ 100%");
     * </pre>
     * 
     * @param name the profil's name
     * @param entries an list of formatted profile's entries
     * @return the profile
     */
    public static CpuProfile of(final String name, final List<String> entries) {
        return new CpuProfile(
                    name, 
                    entries
                      .stream()
                      .map(e -> CpuProfileEntry.parse(e))
                      .collect(Collectors.toList()));
    }

    /**
     * Returns the default profile: "00:00-23:59 @ 100%"
     * 
     * @return the default profile
     */
    public static CpuProfile defaultProfile() {
        return CpuProfile.of("default", toList("00:00-23:59 @ 100%"));
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


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        entries.forEach(e -> {
            sb.append(e.toString());
            sb.append(System.lineSeparator());
        });
        return sb.toString();
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
