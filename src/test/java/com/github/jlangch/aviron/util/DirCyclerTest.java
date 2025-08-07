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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;


public class DirCyclerTest {

    @Test 
    void testNextDirEmpty() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            assertNull(cycler.nextDir());
        }
    }

    @Test 
    void testNextDirOne() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            demoFS.createFilestoreSubDir("000");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());
        }
    }

    @Test 
    void testNextDirMany() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());
            assertEquals("002", cycler.nextDir().getName());
            
            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());
            assertEquals("002", cycler.nextDir().getName());
            
            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());
            assertEquals("002", cycler.nextDir().getName());
        }
    }

    @Test 
    void testNextDirGrowing() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            assertNull(cycler.nextDir());

            demoFS.createFilestoreSubDir("000");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());

            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());
            assertEquals("002", cycler.nextDir().getName());
            
            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());
            assertEquals("002", cycler.nextDir().getName());
        }
    }

    @Test 
    void testDirs() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            List<File> dirs = cycler.dirs();
            assertEquals(0, dirs.size());

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");

            dirs = cycler.dirs();
            assertEquals(3, dirs.size());
            assertEquals("000", dirs.get(0).getName());
            assertEquals("001", dirs.get(1).getName());
            assertEquals("002", dirs.get(2).getName());
        }
    }

    @Test 
    void testSaveAnRestoreLastDir() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");
            demoFS.createFilestoreSubDir("003");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());

            // save point
            final String last = cycler.lastDirName();
            assertEquals("001", last);

            cycler.refresh();
            assertNull(cycler.lastDirName());

            // restore
            cycler.restoreLastDirName(last);
            assertEquals(last, cycler.lastDirName());

            assertEquals("002", cycler.nextDir().getName());
            assertEquals("003", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());
        }
    }

    @Test 
    void testLoadAndRestoreState() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final File cyclerStateFile = new File(demoFS.getRootDir(), "filestore-cycler.state");

            final IDirCycler cycler = demoFS.getFilestoreDirCycler();

            assertNull(cycler.lastDirName());

            // load state from non existing state file
            cycler.loadStateFromFile(cyclerStateFile);
            assertNull(cycler.lastDirName());

            // save point
            cycler.saveStateToFile(cyclerStateFile);
            cycler.refresh();
            cycler.loadStateFromFile(cyclerStateFile);
            assertNull(cycler.lastDirName());
            
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");
            demoFS.createFilestoreSubDir("003");

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());

            // save point
            assertEquals("001", cycler.lastDirName());
            cycler.saveStateToFile(cyclerStateFile);
            cycler.refresh();
            cycler.loadStateFromFile(cyclerStateFile);
            assertEquals("001", cycler.lastDirName());

            cycler.refresh();
            assertNull(cycler.lastDirName());

            // restore
            cycler.restoreLastDirName("002");
            assertEquals("002", cycler.lastDirName());

            assertEquals("003", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());
        }
    }


    @Test 
    void testAutoStoreState() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            final File cyclerStateFile = new File(demoFS.getRootDir(), "filestore-cycler.state");

            IDirCycler cycler = new DirCycler(demoFS.getFilestoreDir(), cyclerStateFile);
            assertNull(cycler.lastDirName());

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");
            demoFS.createFilestoreSubDir("002");
            demoFS.createFilestoreSubDir("003");

            // start where the cycler left off
            cycler = new DirCycler(demoFS.getFilestoreDir(), cyclerStateFile);
            assertNull(cycler.lastDirName());

            assertEquals("000", cycler.nextDir().getName());
            assertEquals("001", cycler.nextDir().getName());

            // start where the cycler left off
            cycler = new DirCycler(demoFS.getFilestoreDir(), cyclerStateFile);
            assertEquals("001", cycler.lastDirName());

            cycler.refresh();
            assertNull(cycler.lastDirName());

            cycler.refresh();
            cycler = new DirCycler(demoFS.getFilestoreDir(), cyclerStateFile);
            assertNull(cycler.lastDirName());

            // restore
            cycler.restoreLastDirName("002");
            assertEquals("002", cycler.lastDirName());

            assertEquals("003", cycler.nextDir().getName());
            assertEquals("000", cycler.nextDir().getName());
        }
    }

}
