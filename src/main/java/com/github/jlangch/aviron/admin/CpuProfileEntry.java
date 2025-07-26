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

import com.github.jlangch.aviron.ex.AvironException;


public class CpuProfileEntry {

    public CpuProfileEntry(final LocalTime start, final LocalTime end, final int limit) {
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

    public static CpuProfileEntry parse(final String s) {
        try {
            final String[] e = s.split(" *[@:\\-%] *");
            return new CpuProfileEntry(
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

    public boolean isOverlapping(final CpuProfileEntry other) {
        if (other == null) {
            throw new IllegalArgumentException("A other time entry must not be null!");
        }

        if (other.getEnd().isBefore(start)) return false;
        else if (other.getStart().isAfter(end)) return false;
        else return true;
    }

    public boolean isBefore(final CpuProfileEntry other) {
        if (other == null) {
            throw new IllegalArgumentException("An other entry must not be null!");
        }

        return end.isBefore(other.getStart());
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
