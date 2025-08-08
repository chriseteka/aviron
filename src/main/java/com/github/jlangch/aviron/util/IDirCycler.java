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
import java.time.LocalDateTime;
import java.util.List;


/**
 * Cycles through all the subdirectories of a directory. If the last 
 * subdirectory is reached it starts over with the first subdirectory.
 * 
 * <p>The dir cycler helps breaking down large file store scans into smaller
 * chunks. 
 * 
 * <p>Recursively scanning a large file store in a single run can cause the 
 * clamd daemon to take many hours to return results, delaying the quarantine 
 * of infected files.
 * 
 * <p>For faster and more responsive scanning, large file stores should be 
 * divided into smaller chunks. In most cases, file stores are already 
 * segmented into subdirectories to prevent exceeding filesystem limits on 
 * the number of files per directory. These subdirectories are ideal for 
 * defining scan chunks.
 */
public interface IDirCycler {

    /**
     * Returns the root dir
     * 
     * @return the cycler's root dir
     */
    File rootDir();

    /**
     * Returns the next dir in the cycle
     * 
     * @return the next dir or <code>null</code> if the root dir does not have 
     *         any sub dirs
     */
    File nextDir() ;

    /**
     * Refreshes the cycler. Rescans all sub dirs.
     */
    void refresh();

    /**
     * Returns the last sub dir name in the cycle.
     * 
     * @return the last sub dir or <code>null</code> if there not yet a call 
     *         to next()
     */
    String lastDirName();

    /**
     * Returns the timestamp the last sub dir was run.
     * 
     * @return a timestamp or <code>null</code> if not available;
     */
    LocalDateTime lastDirTimestamp();

    /**
     * Returns a list of the managed subdirs
     * 
     * @return a list of the managed subdirs
     */
    public List<File> dirs();

    /**
     * Restores the cycler's last processed sub dir with the passed dir
     * 
     * @param dirName A sub dir name (may be <code>null</code>)
     */
    void restoreLastDirName(String dirName);

    /**
     * Loads cycler's state from a file. The file may be empty or not existing.
     * 
     * @param file a file
     */
    void loadStateFromFile(File file);

    /**
     * Saves the cycler's state to a file.
     * 
     * @param file a file
     */
    void saveStateToFile(File file);
    
}
