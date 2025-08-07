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
package com.github.jlangch.aviron.limiter;

import java.io.File;
import java.util.function.Supplier;

import com.github.jlangch.aviron.admin.ClamdAdmin;
import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Abstraction for a clamd daemon PID
 * 
 * <p>A clamd daemon PID can be specified as:
 * <ul>
 *    <li>a static PID</li>
 *    <li>a PID file to retrieve the PID on demand from the file</li>
 *    <li>a PID supplier function to retrieve the PID on demand</li>
 * </ul>
 */
public class ClamdPid {

    public ClamdPid(final String pid) {
        this.pid = pid;
        this.pidFile = null;
        this.pidSupplier = null;
    }

    public ClamdPid(final File pidFile) {
        this.pid = null;
        this.pidFile = pidFile;
        this.pidSupplier = null;
    }

    public ClamdPid(final Supplier<String> pidSupplier) {
        this.pid = null;
        this.pidFile = null;
        this.pidSupplier = pidSupplier;
    }


    /**
     * Returns the clamd daemon PID from the specified strategy.
     * 
     * <p>Gets the PID through the supplied strategy and checks if a process
     * is running with the obtained PID.
     * 
     * @return the clamd daemon PID or <code>null</code> if the
     *         daemon is not running
     */
    public String getPid() {
        final String pid = getRawPid();
        return StringUtils.isBlank(pid)
                ? null
                : ClamdAdmin.isProcessAlive(pid) ? pid : null;
    }


    private String getRawPid() {
        if (pidFile != null) {
           return ClamdAdmin.loadClamdPID(pidFile);
        }
        else if (pidSupplier != null) {
            try {
                return pidSupplier.get();
            }
            catch(Exception ex) {
                return null;
            }
        }
        else {
            return pid;
        }
    }


    private final String pid;
    private final File pidFile;
    private final Supplier<String> pidSupplier;
}
