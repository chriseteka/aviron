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

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.AvironException;


public class DirCycler implements IDirCycler {

    /**
     * Create a DirCycler on a directory.
     * 
     * @param rootDir the mandatory root directory
     */
    public DirCycler(final File rootDir) {
        this(rootDir, null);
    }

    /**
     * Create a DirCycler on a directory with an optional state file. 
     * 
     * <p>If the state file is given (it must not necessarily exist) the
     * cycler saves its state automatically to this file. 
     * 
     * <p>If the state file exists at construction time the cycler loads
     * the state from from the file and proceeds where it has left off.
     * 
     * @param rootDir  the mandatory root directory
     * @param stateFile an optional state file
     */
    public DirCycler(final File rootDir, final File stateFile) {
        if (rootDir == null) {
            throw new IllegalArgumentException("The rootDir must not be null!");
        }
        if (!rootDir.isDirectory()) {
            throw new RuntimeException("The rootDir does not exist or is not a directory");
        }

        this.rootDir = rootDir;
        this.stateFile = stateFile;

        refreshDirs();

        if (stateFile != null) {
            loadStateFromFile(stateFile);
        }
    }


    @Override
    public File rootDir() {
        return rootDir;
    }

    @Override
    public File nextDir() {
        if (subDirs.isEmpty()) {
            refreshDirs();  // check if new filestore dirs arrived
        }

        if (subDirs.isEmpty()) {
            return null;  // still empty
        }

        int dirIdx = lastDirIdx + 1;
        if (dirIdx >= subDirs.size()) {
            // we past the last directory -> refresh to reflect dir changes
            refreshDirs();
            if (subDirs.isEmpty()) {
                return null;  // empty now
            }
            dirIdx = 0;
        }

        lastDirIdx = dirIdx;
        final File next = subDirs.get(dirIdx);

        if (stateFile != null) {
            saveStateToFile(stateFile);
        }

        return next;
    }

    @Override
    public void refresh() {
        refreshDirs();

        if (stateFile != null) {
            saveStateToFile(stateFile);
        }
    }

    @Override
    public String lastDirName() {
        return lastDirIdx < 0 || lastDirIdx >= subDirs.size()-1
                ? null
                : subDirs.get(lastDirIdx).getName();
    }

    @Override
    public void restoreLastDirName(final String name) {
        refreshDirs();
        lastDirIdx = getIndexOf(name);
        
        if (stateFile != null) {
            saveStateToFile(stateFile);
        }
    }

    @Override
    public void loadStateFromFile(final File file) {
        if (file == null) {
            throw new IllegalArgumentException("The file must not be null!");
        }
 
        if (Files.isRegularFile(file.toPath())) {
            try {
                final String lastDir = new String(
                                            Files.readAllBytes(file.toPath()),
                                            Charset.forName("UTF-8"));

                restoreLastDirName(lastDir);
            }
            catch(Exception ex) {
                throw new AvironException(
                        "Failed to load DirCycler state from file", ex);
            }
        }
        else {
            restoreLastDirName(null);
        }
    }

    @Override
    public void saveStateToFile(final File file) {
        if (file == null) {
            throw new IllegalArgumentException("The file must not be null!");
        }

        final String lastDir = lastDirName();

        try {
            final String data = lastDir == null ? "" : lastDir;
            Files.write(file.toPath(), data.getBytes(Charset.forName("UTF-8")));
        }
        catch(Exception ex) {
            throw new AvironException(
                    "Failed to save DirCycler state to file", ex);
        }
    }

    @Override
    public List<File> dirs() {
        return Arrays.stream(rootDir.listFiles())
                     .filter(f -> f.isDirectory())
                     .sorted()
                     .collect(Collectors.toList());
    }


    private void refreshDirs() {
        subDirs.clear();
        subDirs.addAll(dirs());
        lastDirIdx = -1;
    }

    private int getIndexOf(final String name) {
        if (name != null) {
            for(int ii=0; ii<subDirs.size(); ii++) {
                if (subDirs.get(ii).getName().equals(name)) {
                    return ii;
                }
           }
        }
        return -1;
    }


    private final File rootDir;
    private final File stateFile;
    private int lastDirIdx = -1;
    private final List<File> subDirs = new ArrayList<>();
}
