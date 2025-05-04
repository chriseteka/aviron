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
package com.github.jlangch.aviron;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.ex.NotRunningException;
import com.github.jlangch.aviron.util.OS;
import com.github.jlangch.aviron.util.Shell;
import com.github.jlangch.aviron.util.ShellResult;
import com.github.jlangch.aviron.util.StringUtils;
import com.github.jlangch.aviron.util.Version;


/**
 * Offers functions to control the CPU limit of the <i>clamd</i> daemon.
 * 
 * <p>
 * The <i>cpulimit</i> tool must be installed on the host running the
 * <i>clamd</i> daemon:
 * 
 * <pre>
 * Alma Linux:         dnf install cpulimit
 * MacOS (Homebrew):   brew install cpulimit
 * </pre>
 */
public class Admin {

    /**
     * @return the Aviron version
     */
    public static String getAvironVersion() {
        return Version.VERSION;
    }

    /**
     * Returns the <i>clamd</i> PID or <code>null</code> if <i>clamd</i> daemon 
     * is not running.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the <i>clamd</i> PID
     */
    public static String getClamdPID() {
        if (OS.isLinux() || OS.isMacOSX()) {
            try {
                final ShellResult r = Shell.execCmd("pgrep", "clamd");
                return r.isZeroExitCode()
                        ? r.getStdoutLines()
                           .stream()
                           .filter(s -> !StringUtils.isBlank(s))
                           .findFirst()
                           .orElse(null)
                        : null;
            }
            catch(IOException ex) {
                throw new AvironException("Failed to get clamd PID", ex);
            }
        }
        else {
            throw new AvironException(
                    "Admin::getClamdPid is available for Linux and MacOS only!");
        }
    }

    /**
     * Returns the <i>cpulimit</i> PIDs. There are two <i>cpulimit</i> processes controlling
     * the CPU limit of a target process.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the list with PIDs
     */
    public static List<String> getCpulimitPIDs() {
        if (OS.isLinux() || OS.isMacOSX()) {
            try {
                final ShellResult r = Shell.execCmd("pgrep", "cpulimit");
                return r.isZeroExitCode()
                        ? r.getStdoutLines()
                           .stream()
                           .filter(s -> !StringUtils.isBlank(s))
                           .collect(Collectors.toList())
                        : null;
            }
            catch(IOException ex) {
                throw new AvironException("Failed to get cpulimit PIDs", ex);
            }
        }
        else {
            throw new AvironException(
                    "Admin::getCpulimitPIDs is available for Linux and MacOS only!");
        }
    }

    /**
     * Activates a CPU limit [0..LIMIT] on the <i>clamd</i> process
     * 
     * <p>The value of LIMIT depends on the number of logical processors: 
     * <ul>
     *  <li>on a 8 core <i>MacBook Air</i> LIMIT is 800%</li>
     *  <li>on a <i>Intel</i> single core with 2 hyperthreads LIMIT is 200%</li>
     * </ul>
     * 
     * <p>
     * Note: If the <i>clamd</i> process terminates the controlling <i>cpulimit</i>
     *       process will terminate automatically as well.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @param limit a percent value 0..LIMIT
     */
    public static void activateCpuLimit(final int limit) {
        if (OS.isLinux() || OS.isMacOSX()) {
            if (limit < 0) {
                throw new IllegalArgumentException(
                        "A limit value must not be negative!");
            }

            try {
                final String pid = getClamdPID();
                if (pid == null) {
                    throw new NotRunningException("The clamd daemon is not running!");
                }

                // run cpulimit as background process
                Shell.execCmdBackground("cpulimit", "--limit=" + limit, "--pid=" + pid);
            }
            catch(IOException ex) {
                throw new AvironException(
                        "Failed to activate a CPU limit on the clamd process", ex);
            }
        }
        else {
            throw new AvironException(
                    "Admin::activateCpuLimit is available for Linux and MacOS only!");
        }
    }

    /**
     * Deactivates a CPU limit on the <i>clamd</i> process
     * 
     * <p>
     * Sends a <b>SIGINT</b> to the controlling <i>cpulimit</i> process
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     */
    public static void deactivateCpuLimit() {
        if (OS.isLinux() || OS.isMacOSX()) {
            final List<String> pids = getCpulimitPIDs();
            if (pids.isEmpty()) {
                throw new NotRunningException("No cpulimit processes running!");
            }

            final String clamdPID = getClamdPID();

            pids.forEach(pid -> {
                try {
                    final ShellResult r = Shell.execCmd("kill", "-SIGINT", pid);
                    if (!r.isZeroExitCode()) {
                        throw new AvironException(
                                "Failed to deactivate the CPU limit on the clamd "
                                + "process (" + clamdPID + ").\n"
                                + "\nExit code: " + r.getExitCode()
                                + "\nError msg: " + r.getStderr());
                    }
                }
                catch(IOException ex) {
                    throw new AvironException(
                            "Failed to deactivate a CPU limit on the clamd process", ex);
                }
            });
        }
        else {
            throw new AvironException(
                    "Admin::deactivateCpuLimit is available for Linux and MacOS only!");
        }
    }

    /**
     * Returns the number of available processors or the number of hyperthreads 
     * if the CPU supports hyperthreads.
     * 
     * <pre>
     * Linux shell:    nproc --all
     * MacOS shell:    sysctl -n hw.ncpu
     * Java:           Runtime.getRuntime().availableProcessors()
     * </pre>
     * 
     * @return the number of CPUs
     */
    public static int getNrOfCpus() {
        return Runtime.getRuntime().availableProcessors();
    }

}
