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


/**
 * Cycles through all the subdirs of a directory. If the last subdir is
 * reached it starts over with the first subdir.
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
     * Restores the cycler's state the passed dir name as last processed
     * sub dir.
     * 
     * @param name A sub dir name (may be <code>null</code>)
     */
    void restoreLastDirName(String dirName);

}
