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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A FileStoreMgr keeps track of the directories in a file store.
 * 
 * For each call to {@link #nextDir() nextDir} it cycles through the
 * directories of the file store one by one. If the last directory 
 * is reached it starts over from the first directory.
 * 
 * <p>The directories are ordered ascending by its name.
 * 
 * <p>At start over event it refreshes the list of the directories to
 * respect newly created directories.
 * 
 * <pre>
 * /data/filestore/
 *   |
 *   +-- 0000        + &lt;--+
 *   +-- 0001        |    |
 *   :               |    |
 *   +-- NNNN        v ---+
 * </pre>
 */
public class FileStoreMgr {

    public FileStoreMgr(final File rootDir) {
       this.rootDir = rootDir;
     }

    public File getRootDir() {
        return rootDir;
    }

    public File nextDir() {
        if (subDirs.isEmpty()) {
            refresh();  // check if new filestore dirs arrived
        }

        if (subDirs.isEmpty()) {
            return null;  // still empty
        }

        int dirIdx = lastDirIdx + 1;
        if (dirIdx >= subDirs.size()) {
            // we past the last directory -> refresh to reflect dir changes
            refresh();
            if (subDirs.isEmpty()) {
                return null;  // empty now
            }
            dirIdx = 0;
        }

        lastDirIdx = dirIdx;
        return subDirs.get(dirIdx);
    }

    public void refresh() {
        subDirs.clear();
        subDirs.addAll(dirs());
        lastDirIdx = -1;
    }

    public String getLastDirName() {
        return lastDirIdx < 0 || lastDirIdx >= subDirs.size()-1
                ? null
                : subDirs.get(lastDirIdx).getName();
    }

    public void restoreLastDirName(final String name) {
        lastDirIdx = getIndexOf(name);
    }


    private List<File> dirs() {
        return Arrays.stream(rootDir.listFiles())
                     .filter(f -> f.isDirectory())
                     .sorted()
                     .collect(Collectors.toList());
    }

    private int getIndexOf(final String name) {
        for(int ii=0; ii<subDirs.size(); ii++) {
            if (subDirs.get(ii).getName().equals(name)) {
                return ii;
            }
        }
        return -1;
    }


    private final File rootDir;

    private int lastDirIdx = -1;
    private final List<File> subDirs = new ArrayList<>();
}
