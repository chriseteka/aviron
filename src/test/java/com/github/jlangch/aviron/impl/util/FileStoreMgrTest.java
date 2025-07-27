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
package com.github.jlangch.aviron.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.impl.test.TempFS;


public class FileStoreMgrTest {

    @Test 
    void testNextDirEmpty() throws IOException {
        final TempFS tempFS = new TempFS();

        final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

        try {
            assertNull(fsMgr.nextDir());
            assertNull(fsMgr.nextDir());
            assertNull(fsMgr.nextDir());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    void testNextDirGrowing() throws IOException {
        final TempFS tempFS = new TempFS();

        final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

        try {
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
        finally {
            tempFS.remove();
        }
    }

    @Test 
    void testNextDirOneDir() throws IOException {
        final TempFS tempFS = new TempFS();

        final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

        try {
            tempFS.createScanSubDir("0000");

            assertEquals("0000", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
            
            assertEquals("0000", fsMgr.nextDir().getName());
        }
        finally {
            tempFS.remove();
        }
    }

    @Test 
    void testNextDir() throws IOException {
        final TempFS tempFS = new TempFS();

        final FileStoreMgr fsMgr = new FileStoreMgr(tempFS.getScanDir());

        try {
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
        finally {
            tempFS.remove();
        }
    }

}
