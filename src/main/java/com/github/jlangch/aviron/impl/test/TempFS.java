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
package com.github.jlangch.aviron.impl.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;


public class TempFS {

    public TempFS() {
        this.root = createTempDir();
        this.scanDir = new File(root, "scan");
        this.quarantineDir = new File(root, "quarantine");
        
        this.scanDir.mkdir();
        this.quarantineDir.mkdir();
    }

    
    public int countScanFiles() {
        return scanDir.list().length;
    }

    public int countQuarantineFiles() {
        return quarantineDir.list().length;
    }

    public File createScanFile(final String name, final String data) {
        return createFile(scanDir, name, data);
    }

    public File createQuarantineFile(final String name, final String data) {
        return createFile(quarantineDir, name, data);
    }

    public File getRoot() {
        return root;
    }

    public File getQuarantineDir() {
        return quarantineDir;
    }

    public File getScanDir() {
        return scanDir;
    }

    public void remove() {
        try {
            Files.walk(root.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to remove tempFS", ex);
        }
    }


    private File createFile(final File dir, final String name, final String data) {
        final File file = new File(dir, name);
        try {
            Files.write(file.toPath(), toBytes(data));
            return file;
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to create file", ex);
        }
    }

    private File createTempDir() {
        try {
           return Files.createTempDirectory("quarantine_").toFile();
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to create tempFS", ex);
        }
    }

    private byte[] toBytes(final String text) {
        return text.getBytes(Charset.defaultCharset());
    }


    private final File root;
    private final File quarantineDir;
    private final File scanDir;
}
