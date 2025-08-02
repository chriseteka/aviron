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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;


public class DemoFilestoreTest {

    @Test
    void testNew() throws IOException {
        File root = null;
        
        try(DemoFilestore demoFS = new DemoFilestore()) {

            root = demoFS.getRootDir();
            
            assertNotNull(demoFS.getRootDir());
            assertNotNull(demoFS.getFilestoreDir());
            assertNotNull(demoFS.getQuarantineDir());
            
            assertTrue(demoFS.getRootDir().isDirectory());
            assertTrue(demoFS.getFilestoreDir().isDirectory());
            assertTrue(demoFS.getQuarantineDir().isDirectory());
        }

        // DemoFilestore must have been deleted
        assertFalse(root.isDirectory());
    }
    
    @Test
    void testDirs() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {

            final File d1 = demoFS.createFilestoreSubDir("000");
            final File d2 = demoFS.createFilestoreSubDir("001");
                        
            assertTrue(d1.isDirectory());
            assertTrue(d2.isDirectory());
        }
    }
    
    @Test
    void testFiles() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final File f1 = demoFS.createFilestoreFile("000", "1.txt");
            final File f2 = demoFS.touchFilestoreFile("000", "2.txt");
            final File f3a = demoFS.createFilestoreFile("000", "3.txt");
            final File f3b = demoFS.appendToFilestoreFile("000", "3.txt");

            assertTrue(f1.isFile());
            assertTrue(f2.isFile());
            assertTrue(f3a.isFile());
            assertTrue(f3b.isFile());

            final File f4 = demoFS.createFilestoreFile("001", "1.txt");

            assertTrue(f4.isFile());

            demoFS.deleteFilestoreFile("001", "1.txt");
            
            assertFalse(f4.isFile());
         }
    }
    
    @Test
    void testQuarantine() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            demoFS.createFilestoreFile("000", "1.txt");
           
            assertEquals(0, demoFS.countQuarantineFiles());
         }
    }
    
    @Test
    void testClearQuarantine() throws IOException {
        try(DemoFilestore demoFS = new DemoFilestore()) {

            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            demoFS.createFilestoreFile("000", "1.txt");
           
            assertEquals(0, demoFS.countQuarantineFiles());
            
            demoFS.clearQuarantine();
           
            assertEquals(0, demoFS.countQuarantineFiles());
         }
    }

}
