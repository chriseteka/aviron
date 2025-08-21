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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.impl.util.Shell;
import com.github.jlangch.aviron.impl.util.Signal;
import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Representation for a clamd daemon.
 *
 * <p>A clamd daemon can be specified in terms of its PID as:
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
public class Clamd {

    /**
     * Create a clamd representation from a clamd PID.
     *
     * @param pid a non blank PID
     *
     * @see #Clamd(File)
     * @see #Clamd(Supplier)
     * @see #getPids()
     */
    public Clamd(final String pid) {
        StringUtils.requireNonBlank(pid, "A pid must not be blank!");

        this.pidStatic = pid;
        this.pidFile = null;
        this.pidSupplier = null;
    }

    /**
     * Create a clamd representation from a clamd PID file.
     *
     * @param pidFile a mandatory PID file
     *
     * @see #Clamd(String)
     * @see #Clamd(Supplier)
     * @see #getPids()
     */
    public Clamd(final File pidFile) {
    	Objects.requireNonNull(pidFile, "A pidFile must not be null!");

        this.pidStatic = null;
        this.pidFile = pidFile;
        this.pidSupplier = null;
    }

    /**
     * Create a clamd representation from a function that returns the clamd PID.
     *
     * @param pidSupplier a mandatory PID supplier function
     *
     * @see #Clamd(String)
     * @see #Clamd(File)
     * @see #getPids()
     */
    public Clamd(final Supplier<String> pidSupplier) {
    	Objects.requireNonNull(pidSupplier, "A pidSupplier must not be null!");

        this.pidStatic = null;
        this.pidFile = null;
        this.pidSupplier = pidSupplier;
    }


    /**
     * Returns the clamd daemon PID.
     *
     * <p>Gets the PID through the supplied strategy and checks if a process
     * with the obtained PID is running otherwise it returns <code>null</code>.
     *
     * <p>
     * Note: This function is available for Linux and MacOS only!
     *
     * @return the clamd daemon PID or <code>null</code> if the daemon is not
     *         running
     *
     * @see #getPids()
     */
    public String getPid() {
        Shell.validateLinuxOrMacOSX("Clamd::getPid");

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
        Shell.validateLinuxOrMacOSX("Clamd::isProcessAlive");

        final String pid = getRawPid();
        return StringUtils.isBlank(pid)
                 ? null
                 : Shell.isProcessAlive(pid);
    }

    /**
     * Activates a CPU limit [0..LIMIT] on the <i>clamd</i> process represented
     * by this <code>Clamd</code>.
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
     * Note: If the <i>clamd</i> process terminates, the controlling <i>cpulimit</i>
     *       process will terminate automatically as well.
     *
     * <p>
     * <b>Setting CPU limits below 20% is generally impractical. You can experiment with it, but
     * experience will likely lead you to the same conclusion.</b>
     *
     * <p>
     * It’s more effective to skip sending scan jobs to the clamd daemon if the CPU limit is under
     * 20%.
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
    	if (limit < 0) {
    		throw new IllegalArgumentException("A CPU limit must not below 0%!");
    	}

        Shell.validateLinuxOrMacOSX("Clamd::activateCpuLimit");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            // kill a possibly running cpulimit process before starting a new one
            deactivateCpuLimit(pid);

            activateCpuLimit(pid, limit);
        }
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process represented by this
     * <code>Clamd</code>.
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
        Shell.validateLinuxOrMacOSX("Clamd::deactivateCpuLimit");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            deactivateCpuLimit(pid);
        }
    }

    /**
     * Kills the <i>clamd</i> process represented by this <code>Clamd</code>.
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
        Shell.validateLinuxOrMacOSX("Clamd::kill");

        final String pid = getRawPid();
        if (StringUtils.isNotBlank(pid)) {
            Shell.kill(Signal.SIGTERM, pid);
        }
    }

    /**
     * Returns the PIDs of all running <i>cpulimit</i> processes.
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
    public static List<String> getCpulimitPids() {
        Shell.validateLinuxOrMacOSX("Clamd::getCpulimitPids");

        return Shell.pgrep("cpulimit");
    }

    /**
     * Returns the PIDs of all running <i>clamd</i> processes.
     *
     * <p>Runs the following shell command to get the pid:
     * <pre>
     * pgrep -x clamd
     * </pre>
     *
     * <p>
     * Note: This function is available for Linux and MacOS only!
     *
     * @return the list with PIDs
     */
    public static List<String> getPids() {
        Shell.validateLinuxOrMacOSX("Clamd::getPids");

        return Shell.pgrep("clamd");
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
           return loadFromPidFile(pidFile);
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
            return pidStatic;
        }
    }

    private void deactivateCpuLimit(final String pid) {
        try {
            // best effort, do not check the exit code
            //
            // note: if there are no cpulimit processes running on the clamd with
            //       the pid <code>pkill</code> returns the exit code 1. we don't
        	//       want to throw an exception in this case
            Shell.execCmd("pkill", "-f", "cpulimit.*" + pid);
        }
        catch(IOException ex) {
            throw new AvironException(
                    "Failed to deactivate CPU limit on the clamd process "
                     + "with the PID " + pid,
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
                        "Failed to activate a CPU limit of " + limit +"% "
                         + "on the clamd process with the PID " + pid,
                        ex);
            }
        }
    }

    private String loadFromPidFile(final File pidFile) {
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


    private final String pidStatic;
    private final File pidFile;
    private final Supplier<String> pidSupplier;
}
