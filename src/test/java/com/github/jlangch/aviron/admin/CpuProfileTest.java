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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.admin.CpuProfile.Entry;


class CpuProfileTest {

    @Test
    void testCpuProfileEntryNew() {
        assertThrows(
            IllegalArgumentException.class, 
            () -> new Entry(null, LocalTime.of(15, 0), 100));

        assertThrows(
                IllegalArgumentException.class, 
                () -> new Entry(LocalTime.of(14, 0), null, 100));

        assertThrows(
                IllegalArgumentException.class, 
                () -> new Entry(LocalTime.of(14, 0), LocalTime.of(15, 0), -1));

        assertThrows(
                IllegalArgumentException.class, 
                () -> new Entry(LocalTime.of(16, 0), LocalTime.of(15, 0), 100));
    }

    @Test
    void testCpuProfileEntryToString() {
        assertEquals("00:00 - 03:09 @ 10%", new Entry(LocalTime.of(0, 0), LocalTime.of(3, 9), 10).toString());
        assertEquals("12:00 - 15:00 @ 100%", new Entry(LocalTime.of(12, 0), LocalTime.of(15, 0), 100).toString());
    }

    @Test
    void testCpuProfileEntryParse() {
        assertEquals("00:00 - 03:09 @ 10%", Entry.parse("00:00 - 03:09 @ 10%").toString());
        assertEquals("12:00 - 15:00 @ 100%", Entry.parse("12:00 - 15:00 @ 100%").toString());
    }

    @Test
    void testCpuProfileEntryWithin() {
        final Entry entry = new Entry(LocalTime.of(14, 0), LocalTime.of(14, 59), 100);

        assertFalse(entry.isWithin(LocalTime.of(13, 0)));
        assertFalse(entry.isWithin(LocalTime.of(13, 59)));

        assertTrue(entry.isWithin(LocalTime.of(14, 0)));
        assertTrue(entry.isWithin(LocalTime.of(14, 30)));
        assertTrue(entry.isWithin(LocalTime.of(14, 59)));

        assertFalse(entry.isWithin(LocalTime.of(15, 0)));
        assertFalse(entry.isWithin(LocalTime.of(15, 30)));
    }


    @Test
    void testCpuProfileEntryOverlapping() {
        final Entry entry = new Entry(LocalTime.of(14, 0), LocalTime.of(14, 59), 100);

        assertFalse(entry.isOverlapping(new Entry(LocalTime.of(12, 0), LocalTime.of(13, 0), 100)));
        assertFalse(entry.isOverlapping(new Entry(LocalTime.of(13, 0), LocalTime.of(13, 59), 100)));
 
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(13, 00), LocalTime.of(16, 00), 100)));

        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(13, 50), LocalTime.of(14, 00), 100)));
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(13, 50), LocalTime.of(14, 10), 100)));

        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 00), LocalTime.of(14, 20), 100)));
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 20), LocalTime.of(14, 40), 100)));
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 40), LocalTime.of(14, 59), 100)));
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 00), LocalTime.of(14, 59), 100)));
        
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 50), LocalTime.of(15, 00), 100)));
        assertTrue(entry.isOverlapping(new Entry(LocalTime.of(14, 50), LocalTime.of(15, 10), 100)));

        assertFalse(entry.isOverlapping(new Entry(LocalTime.of(15, 0), LocalTime.of(16, 0), 100)));
        assertFalse(entry.isOverlapping(new Entry(LocalTime.of(16, 0), LocalTime.of(17, 0), 100)));
    }
    
    @Test
    void testCpuProfileNew() {
        final List<Entry> entries = new ArrayList<>();
        entries.add(Entry.parse("00:00-05:59 @ 100%"));
        entries.add(Entry.parse("06:00-08:59 @ 50%"));
        entries.add(Entry.parse("09:00-17:59 @ 0%"));
        entries.add(Entry.parse("18:00-21:59 @ 50%"));
        entries.add(Entry.parse("22:00-23:59 @ 100%"));

        final CpuProfile profile = new CpuProfile("weekday", entries);
        assertEquals("weekday", profile.getName());
        assertEquals(5, profile.getEntries().size());
    }

    @Test
    void testCpuProfileNewUnordered() {
        final List<Entry> entries = new ArrayList<>();
        entries.add(Entry.parse("00:00-05:59 @ 100%"));
        entries.add(Entry.parse("09:00-17:59 @ 0%"));
        entries.add(Entry.parse("06:00-08:59 @ 50%"));
        entries.add(Entry.parse("18:00-21:59 @ 50%"));
        entries.add(Entry.parse("22:00-23:59 @ 100%"));
 
        assertThrows(
                IllegalArgumentException.class, 
                () -> new CpuProfile("weekday", entries));
    }
    
    @Test
    void testCpuProfileNew2() {
        final CpuProfile profile = new CpuProfile(
                                        "weekday", 
                                        "00:00-05:59 @ 100%, " +
                                        "06:00-08:59 @ 50%, " +
                                        "09:00-17:59 @ 0%, " +
                                        "18:00-21:59 @ 50%, " +
                                        "22:00-23:59 @ 100%");
        
        assertEquals("weekday", profile.getName());
        assertEquals(5, profile.getEntries().size());
    }

    @Test
    void testCpuProfileNewUnordered2() {
        assertThrows(
                IllegalArgumentException.class, 
                () -> new CpuProfile(
                            "weekday", 
                            "00:00-05:59 @ 100%, " +
                            "09:00-17:59 @ 0%, " +
                            "06:00-08:59 @ 50%, " +
                            "18:00-21:59 @ 50%, " +
                            "22:00-23:59 @ 100%"));
    }
    
    @Test
    void testCpuProfileLimit1() {
        final List<Entry> entries = new ArrayList<>();
        entries.add(Entry.parse("00:00-05:59 @ 100%"));
        entries.add(Entry.parse("06:00-08:59 @ 50%"));
        entries.add(Entry.parse("09:00-17:59 @ 0%"));
        entries.add(Entry.parse("18:00-21:59 @ 50%"));
        entries.add(Entry.parse("22:00-23:59 @ 100%"));
        
        final CpuProfile profile = new CpuProfile("weekday", entries);

        assertEquals(100, profile.getLimit(LocalTime.of(0, 0)));
        assertEquals(100, profile.getLimit(LocalTime.of(3, 11)));
        assertEquals(100, profile.getLimit(LocalTime.of(5, 59)));
 
        assertEquals(50, profile.getLimit(LocalTime.of(6, 0)));
        assertEquals(50, profile.getLimit(LocalTime.of(7, 11)));
        assertEquals(50, profile.getLimit(LocalTime.of(8, 59)));

        assertEquals(0, profile.getLimit(LocalTime.of(9, 0)));
        assertEquals(0, profile.getLimit(LocalTime.of(12, 11)));
        assertEquals(0, profile.getLimit(LocalTime.of(17, 59)));

        assertEquals(50, profile.getLimit(LocalTime.of(18, 0)));
        assertEquals(50, profile.getLimit(LocalTime.of(19, 11)));
        assertEquals(50, profile.getLimit(LocalTime.of(21, 59)));

        assertEquals(100, profile.getLimit(LocalTime.of(22, 0)));
        assertEquals(100, profile.getLimit(LocalTime.of(23, 11)));
        assertEquals(100, profile.getLimit(LocalTime.of(23, 59)));
    }
    
    @Test
    void testCpuProfileLimit2() {
        final List<Entry> entries = new ArrayList<>();
        entries.add(Entry.parse("00:00-05:59 @ 100%"));
        entries.add(Entry.parse("22:00-23:59 @ 100%"));

        final CpuProfile profile = new CpuProfile("weekday", entries);

        assertEquals(100, profile.getLimit(LocalTime.of(0, 0)));
        assertEquals(100, profile.getLimit(LocalTime.of(3, 11)));
        assertEquals(100, profile.getLimit(LocalTime.of(5, 59)));

        assertEquals(0, profile.getLimit(LocalTime.of(6, 0)));
        assertEquals(0, profile.getLimit(LocalTime.of(7, 11)));
        assertEquals(0, profile.getLimit(LocalTime.of(8, 59)));

        assertEquals(0, profile.getLimit(LocalTime.of(9, 0)));
        assertEquals(0, profile.getLimit(LocalTime.of(12, 11)));
        assertEquals(0, profile.getLimit(LocalTime.of(17, 59)));

        assertEquals(0, profile.getLimit(LocalTime.of(18, 0)));
        assertEquals(0, profile.getLimit(LocalTime.of(19, 11)));
        assertEquals(0, profile.getLimit(LocalTime.of(21, 59)));

        assertEquals(100, profile.getLimit(LocalTime.of(22, 0)));
        assertEquals(100, profile.getLimit(LocalTime.of(23, 11)));
        assertEquals(100, profile.getLimit(LocalTime.of(23, 59)));
    }

}
