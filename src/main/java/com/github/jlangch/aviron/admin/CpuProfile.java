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

import static java.lang.Integer.parseInt;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Defines a 24 hours CPU profile
 */
public class CpuProfile {
    
    /**
     * Create a named CPU profile
     * 
     * <pre>
     * final List<Entry> entries = new ArrayList<>();
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
    public CpuProfile(final String name, final List<Entry> entries) {
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
                      .map(e -> Entry.parse(e))
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
 
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
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
    
    
    private void validate(final List<Entry> entries) {
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

    
    public static class Entry {
        public Entry(final LocalTime start, final LocalTime end, final int limit) {
            if (start == null) {
                throw new IllegalArgumentException("A start time must not be null!");
            }
            if (end == null) {
                throw new IllegalArgumentException("An end time must not be null!");
            }
            if (limit < 0) {
                throw new IllegalArgumentException("A limit must not be negative!");
            }
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("The start time must not be after the end time!");
            }

            this.start = start;
            this.end = end;
            this.limit = limit;
        }
        
        public static Entry parse(final String s) {
            try {
                final String[] e = s.split(" *[@:\\-%] *");
                return new Entry(
                            LocalTime.of(parseInt(e[0]), parseInt(e[1])),
                            LocalTime.of(parseInt(e[2]), parseInt(e[3])),
                            parseInt(e[4]));
            }
            catch(Exception ex) {
                throw new AvironException(
                        "Invalid CpuProfile entry. Expected a format like \"09:00 - 15:30 @ 100%\"");
            }
        }

        public boolean isWithin(final LocalTime time) {
            if (time == null) {
                throw new IllegalArgumentException("A time must not be null!");
            }
            
            return !time.isBefore(start) && !time.isAfter(end);
        }

        public boolean isOverlapping(final Entry other) {
            if (other == null) {
                throw new IllegalArgumentException("A other time entry must not be null!");
            }
            
            if (other.end.isBefore(start)) return false;
            else if (other.start.isAfter(end)) return false;
            else return true;
        }

        public boolean isBefore(final Entry other) {
            if (other == null) {
                throw new IllegalArgumentException("An other entry must not be null!");
            }
            
            return end.isBefore(other.start);
        }
      
        public LocalTime getStart() {
            return start;
        }
        public LocalTime getEnd() {
            return end;
        }
        public int getLimit() {
            return limit;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "%02d:%02d - %02d:%02d @ %d%%", 
                    start.getHour(), start.getMinute(),end.getHour(), end.getMinute(), limit);
        }
        

        private final LocalTime start;
        private final LocalTime end;
        private final int limit;
    }

    
    private final static Entry off = new Entry(LocalTime.of(0, 0), LocalTime.of(23, 59), 0);

    private final String name;
    private final List<Entry> entries = new ArrayList<>();
}
