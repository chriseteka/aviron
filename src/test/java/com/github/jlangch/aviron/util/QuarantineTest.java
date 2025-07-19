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

import static com.github.jlangch.aviron.QuarantineFileAction.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.commands.scan.ScanResult;


class QuarantineTest {

    @Test 
    void testTempFS_empty() {
        final TempFS tempFS = new TempFS();

        assertTrue(tempFS.getRoot().isDirectory());

        assertEquals(0, tempFS.countQuarantineFiles());

        tempFS.remove();
        
        assertFalse(tempFS.getRoot().isDirectory());
    }

    @Test 
    void testTempFS_files() throws IOException {
        final TempFS tempFS = new TempFS();

        tempFS.createScanFile("test.data");
        tempFS.createQuarantineFile("test.data");

        assertEquals(1, tempFS.countScanFiles());
        assertEquals(1, tempFS.countQuarantineFiles());

        tempFS.remove();
        
        assertFalse(tempFS.getRoot().isDirectory());
    }

    @Test 
    void testQuarantineNone_1() {
        final TempFS tempFS = new TempFS();
        
        try {
            tempFS.createScanFile("test.data");

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.ok();
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    void testQuarantineNone_2a() {
        final TempFS tempFS = new TempFS();
        
        try {
            tempFS.createScanFile("test.data");

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(emptyVirusMap());
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    void testQuarantineNone_2b() {
        final TempFS tempFS = new TempFS();
        
        try {
            tempFS.createScanFile("test1.data");
            tempFS.createScanFile("test2.data");

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(emptyVirusMap());
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(2, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
        finally {
            tempFS.remove();
        }
    }
 
    
    
    private HashMap<String, List<String>> emptyVirusMap() {
        return new HashMap<String, List<String>>();
    }

    private HashMap<String, List<String>> virusMap(
            final File file, 
            final String virusSignature
    ) {
        final HashMap<String, List<String>> map = new HashMap<>();
        map.put(file.getPath(), toList(virusSignature));
        return map;
    }

    private HashMap<String, List<String>> virusMap(
            final File file1, 
            final String virusSignature1,
            final File file2, 
            final String virusSignature2
    ) {
        final HashMap<String, List<String>> map = new HashMap<>();
        map.put(file1.getPath(), toList(virusSignature1));
        map.put(file2.getPath(), toList(virusSignature2));
        return map;
    }

    private List<String> toList(final String... items) {
        final List<String> list = new ArrayList<>();
        for(String it : items) list.add(it);
        return list;
    }
    
}
