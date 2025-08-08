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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.impl.util.Shell;
import com.github.jlangch.aviron.impl.util.Signal;
import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Abstraction for a clamd daemon represented by a PID.
 * 
 * <p>A clamd daemon PID can be specified as:
 * <ul>
 *    <li>a static PID</li>
 *    <li>a PID file to retrieve the PID on demand from the file</li>
 *    <li>a PID supplier function to retrieve the PID on demand</li>
 * </ul>
 * 
 * <p>
 * In order to use the CPU limit functions the <i>cpulimit</i> tool must 
 * be installed:
 * 
 * <pre>
 * Alma Linux:         » dnf install cpulimit
 * MacOS (Homebrew):   » brew install cpulimit
 * </pre>
 */
public class ClamdPid {

    public ClamdPid(final String pid) {
        this.pid = pid;
        this.pidFile = null;
        this.pidSupplier = null;
    }

    public ClamdPid(final File pidFile) {
        if (pidFile == null) {
            throw new IllegalArgumentException("A pidFile must not be null!");
        }
        this.pid = null;
        this.pidFile = pidFile;
        this.pidSupplier = null;
    }

    public ClamdPid(final Supplier<String> pidSupplier) {
        if (pidSupplier == null) {
            throw new IllegalArgumentException("A pidSupplier must not be null!");
        }
        this.pid = null;
        this.pidFile = null;
        this.pidSupplier = pidSupplier;
    }


    /**
     * Returns the clamd daemon PID.
     * 
     * <p>Gets the PID through the supplied strategy and checks if a process
     * with the obtained PID is running.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the clamd daemon PID or <code>null</code> if the daemon is not 
     *         running
     */
    public String getPid() {
        Shell.validateLinuxOrMacOSX("ClamdPid::getPid");

        final String pid = getRawPid();
        return StringUtils.isBlank(pid)
                ? null
                : Shell.isProcessAlive(pid) ? pid : null;
    }

    /**
     * Checks if the clamd process is running.
     * 
     * <p>Gets the PID through the supplied strategy and checks if a process
     * with the obtained PID is running .

     * <p>Runs the following shell command on the loaded PID:
     * <pre>
     * ps -p ${pid}
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return <code>true</code> if there is process running with the PID else 
     *         <code>false</code> 
     */
    public boolean isProcessAlive() {
        Shell.validateLinuxOrMacOSX("ClamdPid::isProcessAlive");

        final String pid = getRawPid();
        return StringUtils.isBlank(pid)
                 ? null
                 : Shell.isProcessAlive(pid);
    }

    /**
     * Activates a CPU limit [0..LIMIT] on the <i>clamd</i> process represented
     * by this <code>ClamdPid</code>.
     * 
     * <p>If the clamd process is not running the function is silently skipped.
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
     * @param limit a percent value 0..LIMIT
     * 
     * @see #deactivateCpuLimit()
     * @see #getNrOfCpus()
     */
    public void activateCpuLimit(final int limit) {
        Shell.validateLinuxOrMacOSX("ClamdPid::activateCpuLimit");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            // kill a possibly running cpulimit process before starting a new one
            deactivateCpuLimit(pid);
            
            activateCpuLimit(pid, limit);
        }
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process represented by this
     * <code>ClamdPid</code>.
     * 
     * <p>If the clamd process is not running the function is silently skipped.
     * 
     * <p>Runs the following shell command to deactivate the cpu limit:
     * <pre>
     * pkill -f cpulimit.*${clamdPID}
     * </pre>
     * 
     * <p>Note: This function is available for Linux and MacOS only!
     * 
     * @see #activateCpuLimit(int)
     */
    public void deactivateCpuLimit() {
        Shell.validateLinuxOrMacOSX("ClamdPid::deactivateCpuLimit");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            deactivateCpuLimit(pid);
        }
    }

    /**
     * Kills the <i>clamd</i> process represented by this <code>ClamdPid</code>.
     * 
     * <p>Runs the following shell command to kill the process:
     * <pre>
     * kill -15 ${clamdPID}
     * </pre>
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     */
    public void kill() {
        Shell.validateLinuxOrMacOSX("ClamdPid::kill");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            Shell.kill(Signal.SIGTERM, pid);
        }
    }

    /**
     * Returns the PIDs of the running <i>cpulimit</i> processes.
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
        Shell.validateLinuxOrMacOSX("ClamdPid::getCpulimitPIDs");

        return Shell.pgrep("cpulimit");
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


    private String getRawPid() {
        if (pidFile != null) {
           return loadPID(pidFile);
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

    private void deactivateCpuLimit(final String pid) {
        try {
            // best effort, do not check the exit code
            //
            // note: if there are no cpulimit processes running on the {clamdPID} pid
            //       pkill returns the exit code 1. we don't want to throw an exception
            //       in this case
            Shell.execCmd("pkill", "-f", "cpulimit.*" + pid);
        }
        catch(IOException ex) {
            throw new AvironException(
                    "Failed to deactivate CPU limit on the clamd process" + pid, 
                    ex);
        }
    }

    private void activateCpuLimit(final String pid, final int limit) {
        if (limit != 100) {
            try {
                // /bin/sh -c "nohup /usr/bin/cpulimit -p 1234 -l 50 </dev/null &>/dev/null &"

                // run cpulimit as nohup process
                Shell.execCmdBackgroundNohup("cpulimit", "--limit=" + limit, "--pid=" + pid);
            }
            catch(IOException ex) {
                throw new AvironException(
                        "Failed to activate a CPU limit on the clamd process", ex);
            }
        }
    }
    
    private String loadPID(final File pidFile) {
        if (pidFile == null) {
            throw new IllegalArgumentException("A pid file must not be null!");
        }

        try {
            if (pidFile.isFile() && pidFile.canRead()) {
                return Files
                        .lines(pidFile.toPath(), Charset.defaultCharset())
                        .map(l -> l.trim())
                        .findFirst()
                        .orElse(null);
            }
            else {
                return null;
            }
        }
        catch(Exception ex) {
            throw new AvironException(
                    "Failed to load PID from file «" + pidFile + "»", 
                    ex);
        }
    }


    private final String pid;
    private final File pidFile;
    private final Supplier<String> pidSupplier;
}
