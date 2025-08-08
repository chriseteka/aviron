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
package com.github.jlangch.aviron.admin;

import java.io.File;
import java.util.List;

import com.github.jlangch.aviron.impl.util.CollectionUtils;
import com.github.jlangch.aviron.impl.util.Shell;
import com.github.jlangch.aviron.impl.util.StringUtils;
import com.github.jlangch.aviron.limiter.ClamdPid;


/**
 * Offers functions to manage the <i>clamd</i> daemon.
 * 
 * <p>
 * The <i>cpulimit</i> tool must be installed to control the CPU limit
 * of the <i>clamd</i> daemon:
 * 
 * <pre>
 * Alma Linux:         » dnf install cpulimit
 * MacOS (Homebrew):   » brew install cpulimit
 * </pre>
 * 
 * Note: ClamdAdmin is going to be removed in favor of the ClamdPid class!
 * 
 * @deprecated
 */
public class ClamdAdmin {

    /**
     * Returns the <i>clamd</i> PID or <code>null</code> if <i>clamd</i> daemon 
     * is not running.
     * 
     * <p>Runs the following shell command to get the pid:
     * <pre>
     * pgrep -x clamd
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the <i>clamd</i> PID or <code>null</code> if <i>clamd</i> is not running.
     */
    public static String getClamdPID() {
        Shell.validateLinuxOrMacOSX("ClamdAdmin::getClamdPID");

        return CollectionUtils.first(ClamdPid.getPids());
    }

    /**
     * Loads the <i>clamd</i> PID from a file.
     * 
     * @param pidFile the pid file
     * @return the PID or <code>null</code> if the PID file does not exist, 
     *         is not readable or is empty
     */
    public static String loadClamdPID(final File pidFile) {
        if (pidFile == null) {
            throw new IllegalArgumentException("A pid file must not be null!");
        }

        return new ClamdPid(pidFile).getPid();
    }

    /**
     * Checks if the process with a PID is running.
     * 
     * <p>Runs the following shell command:
     * <pre>
     * ps -p ${pid}
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @param pid a pid
     * @return <code>true</code> if there is process running with the PID else <code>false</code> 
     */
    public static boolean isProcessAlive(final String pid) {
        if (StringUtils.isBlank(pid)) {
            throw new IllegalArgumentException("A pid must not be blank!");
        }

        Shell.validateLinuxOrMacOSX("ClamdAdmin::isProcessAlive");

        return new ClamdPid(pid).isProcessAlive();
    }

    /**
     * Checks if the clamd process from a PID file is running.
     * 
     * <p>Runs the following shell command on the loaded PID:
     * <pre>
     * ps -p ${pid}
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @param pidFile the pid file
     * @return <code>true</code> if there is process running with the PID else 
     *         <code>false</code> 
     */
    public static boolean isProcessAlive(final File pidFile) {
        if (pidFile == null) {
            throw new IllegalArgumentException("A pid file must not be null!");
        }

        Shell.validateLinuxOrMacOSX("ClamdAdmin::isProcessAlive");

        return new ClamdPid(pidFile).isProcessAlive();
    }

    /**
     * Returns the <i>cpulimit</i> PIDs. There are two <i>cpulimit</i> processes controlling
     * the CPU limit of a target process.
     * 
     * <p>Runs the following shell command to get the pid:
     * <pre>
     * pgrep -x cpulimit
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the list with PIDs
     */
    public static List<String> getCpulimitPIDs() {
        Shell.validateLinuxOrMacOSX("ClamdAdmin::getCpulimitPIDs");

        return ClamdPid.getCpulimitPids();
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
     * <p>Runs the following shell command to activate the cpu limit:
     * <pre>
     * /bin/sh -c "nohup cpulimit -p ${clamdPID} -l ${limit} &lt;/dev/null &amp;&gt;/dev/null &amp;"
     * </pre>
     * 
     * <p>
     * Note: If the <i>clamd</i> process terminates the controlling <i>cpulimit</i>
     *       process will terminate automatically as well.
     * 
     * <p>
     * Note: Still facing "Process found but you aren't allowed to control it"
     *       problem on MacOS, even when run with sudo!
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @param clamdPID a clamd pid
     * @param limit a percent value 0..LIMIT
     * 
     * @see ClamdAdmin#deactivateClamdCpuLimit(String)
     */
    public static void activateClamdCpuLimit(
            final String clamdPID, 
            final int limit
    ) {
        Shell.validateLinuxOrMacOSX("ClamdAdmin::activateClamdCpuLimit");

        new ClamdPid(clamdPID).activateCpuLimit(limit);
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process
     * 
     * <p>Runs the following shell command to deactivate the cpu limit:
     * <pre>
     * pkill -f cpulimit.*${clamdPID}
     * </pre>
     * 
     * <p>Note: This function is available for Linux and MacOS only!
     * 
     * @param clamdPID a clamd pid
     * 
     * @see ClamdAdmin#activateClamdCpuLimit(String,int)
     */
    public static void deactivateClamdCpuLimit(final String clamdPID) {
        Shell.validateLinuxOrMacOSX("ClamdAdmin::deactivateClamdCpuLimit");

        new ClamdPid(clamdPID).deactivateCpuLimit();
    }

    /**
     * Kills the <i>clamd</i> process if its running.
     * 
     * <p>Runs the following shell command to kill the process:
     * <pre>
     * kill -15 ${clamdPID}
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     */
    public static void killClamd() {
        Shell.validateLinuxOrMacOSX("ClamdAdmin::killClamd");

        final String pid = CollectionUtils.first(ClamdPid.getPids());
        if (StringUtils.isNotBlank(pid)) {
            new ClamdPid(pid).kill();
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
        return ClamdPid.getNrOfCpus();
    }

}
