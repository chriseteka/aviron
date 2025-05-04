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

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.ex.NotRunningException;
import com.github.jlangch.aviron.util.Shell;
import com.github.jlangch.aviron.util.ShellBackgroundResult;
import com.github.jlangch.aviron.util.Signal;
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
     * @return the Aviron version as {major}.{minor}.{patch} like "1.1.0".
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
        Shell.validateLinuxOrMacOSX("Admin::getClamdPID");

        final List<String> pids = Shell.pgrep("clamd");
        return pids.isEmpty() ? null : pids.get(0);
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
           Shell.validateLinuxOrMacOSX("Admin::getCpulimitPIDs");

        return Shell.pgrep("cpulimit");
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
     * @return the nohup file
     * 
     * @see Admin#deactivateCpuLimit() deactivateCpuLimit
     * @see Admin#getNrOfCpus() getNrOfCpus
     */
    public static ShellBackgroundResult activateCpuLimit(final int limit) {
           Shell.validateLinuxOrMacOSX("Admin::activateCpuLimit");

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
            return Shell.execCmdBackgroundNohup("cpulimit", "--limit=" + limit, "--pid=" + pid);
        }
        catch(IOException ex) {
            throw new AvironException(
                    "Failed to activate a CPU limit on the clamd process", ex);
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
     * 
     * @see Admin#activateCpuLimit(int) activateCpuLimit
     */
    public static void deactivateCpuLimit() {
           Shell.validateLinuxOrMacOSX("Admin::deactivateCpuLimit");

        final List<String> pids = getCpulimitPIDs();
        if (pids.isEmpty()) {
            throw new NotRunningException("No cpulimit processes running!");
        }

        pids.forEach(pid -> {
            try {
                Shell.kill(Signal.SIGINT, pid);
            }
            catch(Exception ex) {
                throw new AvironException(
                        "Failed to deactivate CPU limit on the clamd process", ex);
            }
        });
    }

    /**
     * Kills the <i>clamd</i> process if its running.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     */
    public static void killClamd() {
           Shell.validateLinuxOrMacOSX("Admin::killClamd");

        Shell.kill(Signal.SIGINT, getClamdPID());
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
