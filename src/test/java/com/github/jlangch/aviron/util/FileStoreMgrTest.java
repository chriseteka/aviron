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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.impl.test.TempFS;


public class FileStoreMgrTest {

    @Test 
    void testNextDirEmpty() throws IOException {
        try(TempFS tempFS = new TempFS()) {
            final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

            assertNull(fsMgr.nextDir());
            assertNull(fsMgr.nextDir());
            assertNull(fsMgr.nextDir());
        }
    }

    @Test 
    void testNextDirGrowing() throws IOException {
        try(TempFS tempFS = new TempFS()) {
            final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

            assertNull(fsMgr.nextDir());
            
            tempFS.createScanSubDir("0000");

            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0000", fsMgr.nextDir().getName());

            tempFS.createScanSubDir("0001");
            tempFS.createScanSubDir("0002");

            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());
            assertEquals("0002", fsMgr.nextDir().getName());

            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());
            assertEquals("0002", fsMgr.nextDir().getName());
        }
    }

    @Test 
    void testNextDirOneDir() throws IOException {
        try(TempFS tempFS = new TempFS()) {
            final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

            tempFS.createScanSubDir("0000");

            assertEquals("0000", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
        }
    }

    @Test 
    void testNextDir() throws IOException {
        try(TempFS tempFS = new TempFS()) {
            final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");
            tempFS.createScanSubDir("0002");

            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());
            assertEquals("0002", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());
            assertEquals("0002", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());
            assertEquals("0002", fsMgr.nextDir().getName());
        }
    }


    @Test 
    void testSaveAnRestore() throws IOException {
        try(TempFS tempFS = new TempFS()) {
            final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

            tempFS.createScanSubDir("0000");
            tempFS.createScanSubDir("0001");
            tempFS.createScanSubDir("0002");
            tempFS.createScanSubDir("0003");

            assertEquals("0000", fsMgr.nextDir().getName());
            assertEquals("0001", fsMgr.nextDir().getName());

            // save point
            final String last = fsMgr.getLastDirName();
            assertEquals("0001", last);

            fsMgr.refresh();
            assertNull(fsMgr.getLastDirName());

            // restore
            fsMgr.restoreLastDirName(last);
            assertEquals(last, fsMgr.getLastDirName());

            assertEquals("0002", fsMgr.nextDir().getName());
            assertEquals("0003", fsMgr.nextDir().getName());
            assertEquals("0000", fsMgr.nextDir().getName());
        }
    }

}
