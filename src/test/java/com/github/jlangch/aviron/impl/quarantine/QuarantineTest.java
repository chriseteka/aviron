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
package com.github.jlangch.aviron.impl.quarantine;

import static com.github.jlangch.aviron.events.QuarantineFileAction.COPY;
import static com.github.jlangch.aviron.events.QuarantineFileAction.MOVE;
import static com.github.jlangch.aviron.events.QuarantineFileAction.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.impl.test.EventSink;
import com.github.jlangch.aviron.impl.test.TempFS;


class QuarantineTest {

    @Test 
    void testTempFS_empty() {
        final TempFS tempFS = new TempFS();

        assertTrue(tempFS.getRoot().isDirectory());

        assertEquals(0, tempFS.countQuarantineFiles());

        tempFS.close();
        
        assertFalse(tempFS.getRoot().isDirectory());
    }

    @Test 
    void testTempFS_files() throws IOException {
        final TempFS tempFS = new TempFS();

        final File scanFile1 = tempFS.createScanFile("test.data", "TEST");
        final File quarantineFile1 = tempFS.createQuarantineFile("test.data", "TEST");

        assertTrue(scanFile1.isFile());
        assertTrue(quarantineFile1.isFile());

        assertEquals(1, tempFS.countScanFiles());
        assertEquals(1, tempFS.countQuarantineFiles());

        tempFS.close();
        
        assertFalse(tempFS.getRoot().isDirectory());
    }

    @Test 
    void testQuarantineNone_1a() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test.data", "TEST");

            assertTrue(scanFile1.isFile());

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.ok();
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineNone_1b() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test.data", "TEST");

            assertTrue(scanFile1.isFile());

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(emptyVirusMap());
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineNone_2() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(2, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineListenerNone_1a() {
        try(TempFS tempFS = new TempFS()) {
            final EventSink events = new EventSink();
            
            final File scanFile1 = tempFS.createScanFile("test.data", "TEST");

            assertTrue(scanFile1.isFile());

            final Quarantine quarantine = new Quarantine(NONE, tempFS.getQuarantineDir(), e -> events.process(e));
            
            final ScanResult result = ScanResult.ok();
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
            
            assertEquals(0, events.size());
        }
    }

    @Test 
    void testQuarantineCopy_1a() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
 
            assertTrue(scanFile1.isFile());
 
            final Quarantine quarantine = new Quarantine(COPY, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.ok();
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineCopy_1b() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test.data", "TEST");

            assertTrue(scanFile1.isFile());

            final Quarantine quarantine = new Quarantine(COPY, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(emptyVirusMap());
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineCopy_2a() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(COPY, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(2, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            // analyze data file
            assertEquals("TEST1", data(quarantineFile));
            
            // analyze virus meta data file
            final QuarantineFile qf = QuarantineFile.from(quarantineVirusFile);
            assertEquals(scanFile1.getPath(), qf.getInfectedFile().getPath());
            assertEquals("xxx", qf.getVirusListFormatted());
            assertEquals(COPY, qf.getAction());
            assertNotNull(qf.getQuarantinedAt());
        }
    }

    @Test 
    void testQuarantineCopy_2b() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(COPY, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            quarantine.handleQuarantineActions(result);  // rescan same with COPY -> no quarantine
            
            assertEquals(2, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
            
            final File quarantineFile1 = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile1 = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile1.isFile());
            assertTrue(quarantineVirusFile1.isFile());

            // analyze data file
            assertEquals("TEST1", data(quarantineFile1));
            
            // analyze virus meta data file 1
            final QuarantineFile qf1 = QuarantineFile.from(quarantineVirusFile1);
            assertEquals(scanFile1.getPath(), qf1.getInfectedFile().getPath());
            assertEquals("xxx", qf1.getVirusListFormatted());
            assertEquals(COPY, qf1.getAction());
            assertNotNull(qf1.getQuarantinedAt());
        }
    }

    @Test 
    void testQuarantineListenerCopy_1() {
        try(TempFS tempFS = new TempFS()) {
            final EventSink events = new EventSink();

            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(COPY, tempFS.getQuarantineDir(), e -> events.process(e));
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(2, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            // analyze data file
            assertEquals("TEST1", data(quarantineFile));
            
            // analyze virus meta data file
            final QuarantineFile qf = QuarantineFile.from(quarantineVirusFile);
            assertEquals(scanFile1.getPath(), qf.getInfectedFile().getPath());
            assertEquals("xxx", qf.getVirusListFormatted());
            assertEquals(COPY, qf.getAction());
            assertNotNull(qf.getQuarantinedAt());
                        
            assertEquals(1, events.size());
            
            final QuarantineEvent event = events.events().get(0);
            assertEquals(COPY, event.getAction());
            assertEquals(scanFile1, event.getInfectedFile());
            assertEquals("xxx", event.getVirusList().get(0));
            assertEquals(quarantineFile, event.getQuarantineFile());
            assertNull(event.getException());
        }
    }

    @Test 
    void testQuarantineMove_1a() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
 
            assertTrue(scanFile1.isFile());
 
            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.ok();
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineMove_1b() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test.data", "TEST");

            assertTrue(scanFile1.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(emptyVirusMap());
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(0, tempFS.countQuarantineFiles());
        }
    }

    @Test 
    void testQuarantineMove_2a() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            // analyze data file
            assertEquals("TEST1", data(quarantineFile));
            
            // analyze virus meta data file
            final QuarantineFile qf = QuarantineFile.from(quarantineVirusFile);
            assertEquals(scanFile1.getPath(), qf.getInfectedFile().getPath());
            assertEquals("xxx", qf.getVirusListFormatted());
            assertEquals(MOVE, qf.getAction());
            assertNotNull(qf.getQuarantinedAt());
        }
    }

    @Test 
    void testQuarantineMove_2b() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            final File scanFile1a = tempFS.createScanFile("test1.data", "TEST1");
            assertTrue(scanFile1a.isFile());

            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(4, tempFS.countQuarantineFiles());
            
            final File quarantineFile1 = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile1 = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");
            
            final File quarantineFile2 = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".1");
            final File quarantineVirusFile2 = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".1.virus");

            assertTrue(quarantineFile1.isFile());
            assertTrue(quarantineVirusFile1.isFile());

            assertTrue(quarantineFile2.isFile());
            assertTrue(quarantineVirusFile2.isFile());
            
            // analyze data file
            assertEquals("TEST1", data(quarantineFile1));
            assertEquals("TEST1", data(quarantineFile2));
            
            // analyze virus meta data file 1
            final QuarantineFile qf1 = QuarantineFile.from(quarantineVirusFile1);
            assertEquals(scanFile1.getPath(), qf1.getInfectedFile().getPath());
            assertEquals("xxx", qf1.getVirusListFormatted());
            assertEquals(MOVE, qf1.getAction());
            assertNotNull(qf1.getQuarantinedAt());
            
            // analyze virus meta data file 2
            final QuarantineFile qf2 = QuarantineFile.from(quarantineVirusFile2);
            assertEquals(scanFile1.getPath(), qf2.getInfectedFile().getPath());
            assertEquals("xxx", qf2.getVirusListFormatted());
            assertEquals(MOVE, qf2.getAction());
            assertNotNull(qf2.getQuarantinedAt());
        }
    }

    @Test 
    void testQuarantineListenerMove_1() {
        try(TempFS tempFS = new TempFS()) {
            final EventSink events = new EventSink();

            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), e -> events.process(e));
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            // analyze data file
            assertEquals("TEST1", data(quarantineFile));
 
            
            // analyze virus meta data file
            final QuarantineFile qf = QuarantineFile.from(quarantineVirusFile);
            assertEquals(scanFile1.getPath(), qf.getInfectedFile().getPath());
            assertEquals("xxx", qf.getVirusListFormatted());
            assertEquals(MOVE, qf.getAction());
            assertNotNull(qf.getQuarantinedAt());
            
            assertEquals(1, events.size());
            
            final QuarantineEvent event = events.events().get(0);
            assertEquals(MOVE, event.getAction());
            assertEquals(scanFile1, event.getInfectedFile());
            assertEquals("xxx", event.getVirusList().get(0));
            assertEquals(quarantineFile, event.getQuarantineFile());
            assertNull(event.getException());
        }
    }

    @Test 
    void testQuarantineList() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            final List<QuarantineFile> files = quarantine.listQuarantineFiles();
            assertEquals(1, files.size());
        }
    }

    @Test 
    void testQuarantineRemove() {
        try(TempFS tempFS = new TempFS()) {
            final File scanFile1 = tempFS.createScanFile("test1.data", "TEST1");
            final File scanFile2 = tempFS.createScanFile("test2.data", "TEST2");

            assertTrue(scanFile1.isFile());
            assertTrue(scanFile2.isFile());

            final Quarantine quarantine = new Quarantine(MOVE, tempFS.getQuarantineDir(), null);
            
            final ScanResult result = ScanResult.virusFound(virusMap(scanFile1, "xxx"));
            
            quarantine.handleQuarantineActions(result);
            
            assertEquals(1, tempFS.countScanFiles());
            assertEquals(2, tempFS.countQuarantineFiles());
 
            final File quarantineFile = new File(tempFS.getQuarantineDir(), scanFile1.getName());
            final File quarantineVirusFile = new File(tempFS.getQuarantineDir(), scanFile1.getName() + ".virus");

            assertTrue(quarantineFile.isFile());
            assertTrue(quarantineVirusFile.isFile());
            
            List<QuarantineFile> files = quarantine.listQuarantineFiles();
            assertEquals(1, files.size());
            
            quarantine.removeQuarantineFile(files.get(0));
            
            files = quarantine.listQuarantineFiles();
            assertEquals(0, files.size());          
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

    private List<String> toList(final String... items) {
        final List<String> list = new ArrayList<>();
        for(String it : items) list.add(it);
        return list;
    }

    private String data(final File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to read file", ex);
        }
    }


}
