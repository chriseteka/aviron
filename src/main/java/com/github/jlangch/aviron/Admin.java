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
     * Returns the <i>clamd</i> PID or null if <i>clamd</i> daemon is not running.
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
                if (r.getExitCode() == 0) {
                    return StringUtils
                                .splitIntoLines(r.getStdout())
                                .stream()
                                .filter(s -> !StringUtils.isBlank(s))
                                .findFirst()
                                .orElse(null);
                }
                else {
                    return null;
                }
            }
            catch(IOException ex) {
                throw new AvironException("Failed to get clamd PID", ex);
            }
        }
        else {
            throw new AvironException(
                    "Client::getClamdPid is available for Linux and MacOS only!");
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
                if (r.getExitCode() == 0) {
                    return StringUtils
                                .splitIntoLines(r.getStdout())
                                .stream()
                                .filter(s -> !StringUtils.isBlank(s))
                                .collect(Collectors.toList());
                }
                else {
                    return null;
                }
            }
            catch(IOException ex) {
                throw new AvironException("Failed to get cpulimit PIDs", ex);
            }
        }
        else {
            throw new AvironException(
                    "Client::getCpulimitPIDs is available for Linux and MacOS only!");
        }
    }
    
    /**
     * Activates a CPU limit [0..100%] on the <i>clamd</i> process
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @param limit a percent value 0..100
     */
    public static void activateCpuLimit(final int limit) {
        if (OS.isLinux() || OS.isMacOSX()) {
            if (limit < 0 || limit > 100) {
                throw new IllegalArgumentException(
                		"A limit value must be in the range 0...100!");
            }
            
            try {
                final String pid = getClamdPID();
                if (pid == null) {
                    throw new NotRunningException("The clamd daemon is not running!");
                 }
                
                final ShellResult r = Shell.execCmd(
                						"cpulimit", "--limit" + limit, "--pid=" + pid);
                if (r.getExitCode() != 0) {
                    throw new AvironException(
                            "Failed to activate a CPU limit on the clamd process.\n" +
                            "\nExit code: " + r.getExitCode() +
                            "\nError msg: " + r.getStderr());
                }
            }
            catch(IOException ex) {
                throw new AvironException(
                		"Failed to activate a CPU limit on the clamd process", ex);
            }
        }
        else {
            throw new AvironException(
                    "Client::activateCpuLimit is available for Linux and MacOS only!");
        }
    }
    
    /**
     * Deactivates a CPU limit on the <i>clamd</i> process
     * 
     * <p>
     * Sends a <b>SIGINT</b> to controlling <i>cpulimit</i> process
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
                    if (r.getExitCode() != 0) {
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
                    "Client::deactivateCpuLimit is available for Linux and MacOS only!");
        }
    }

}
