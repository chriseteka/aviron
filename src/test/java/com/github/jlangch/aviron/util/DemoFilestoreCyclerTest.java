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


public class DemoFilestoreCyclerTest {

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
    void testSaveAnRestore() throws IOException {
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

}
