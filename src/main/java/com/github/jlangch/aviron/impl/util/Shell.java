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
package com.github.jlangch.aviron.impl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.util.OS;


public class Shell {

    public static ShellResult execCmd(final String... command) throws IOException {
        final String cmdFormatted = String.join(" ", Arrays.asList(command));

        try {
            final Process proc = Runtime.getRuntime().exec(command);

            return getShellResult(proc);
        }
        catch(Exception ex) {
            throw new AvironException("Failed to run command: " + cmdFormatted, ex);
        }
    }

    public static ShellBackgroundResult execCmdBackground(final String... command) throws IOException {
        validateLinuxOrMacOSX("Shell::execCmdBackground");

        final String cmdFormatted = String.join(" ", Arrays.asList(command));

        try {
            final String cmd = cmdFormatted + " &";

            final Process proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
 
            if (proc.isAlive()) {
                // process is running in background
                return new ShellBackgroundResult(new ShellResult(null, null, 0), null);
            }
            else {
                // process died at starting up
                return new ShellBackgroundResult(new ShellResult(null, null, proc.exitValue()), null);
            }
        }
        catch(Exception ex) {
            throw new AvironException(
                    "Failed to run command: /bin/sh -c \"" + cmdFormatted + "/", 
                    ex);
        }
    }

    public static ShellBackgroundResult execCmdBackgroundNohup(final String... command) throws IOException {
        validateLinuxOrMacOSX("Shell::execCmdNohup");

        final String cmdFormatted = String.join(" ", Arrays.asList(command));

        try {
            final File nohup = File.createTempFile("nohup-", ".out");
            nohup.deleteOnExit();

            final String cmd = "nohup " + cmdFormatted + " </dev/null &>" + nohup.getAbsolutePath() + " &";

            final Process proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});

            if (proc.isAlive()) {
                // process is running in background
                return new ShellBackgroundResult(new ShellResult(null, null, 0), nohup);
            }
            else {
                // process died at starting up -> see nohup file
                return new ShellBackgroundResult(new ShellResult(slurp(nohup), null, proc.exitValue()), nohup);
            }
        }
        catch(Exception ex) {
            throw new AvironException(
                    "Failed to run command: /bin/sh -c \"" + cmdFormatted + "/", 
                    ex);
        }
    }

    public static boolean isProcessAlive(final String pid) {
        validateLinuxOrMacOSX("Shell::isProcessAlive");

        try {
            return Shell.execCmd("ps", "-p", pid)
                        .isZeroExitCode();
        }
        catch(IOException ex) {
            throw new AvironException(
                    "Failed to check if the process with the PID " + pid + " is alive!", 
                    ex);
        }
    }

    public static List<String> pgrep(final String process) {
        validateLinuxOrMacOSX("Shell::pgrep");

        try {
            final ShellResult r = Shell.execCmd("pgrep", "-x", process);
            return r.isZeroExitCode()
                    ? r.getStdoutLines()
                       .stream()
                       .filter(s -> !StringUtils.isBlank(s))
                       .collect(Collectors.toList())
                    : new ArrayList<>();
        }
        catch(IOException ex) {
            throw new AvironException("Failed to get " + process + " PIDs", ex);
        }
    }

    public static void kill(final Signal signal, final String pid) {
        validateLinuxOrMacOSX("Shell::kill");

        if (!StringUtils.isBlank(pid)) {
            try {
                final ShellResult r = Shell.execCmd("kill", "-" + signal.signal(), pid);
                if (!r.isZeroExitCode()) {
                    throw new AvironException(
                            "Failed to kill process (" + pid + ").\n"
                            + "\nExit code: " + r.getExitCode()
                            + "\nError msg: " + r.getStderr());
                }
            }
            catch(IOException ex) {
                throw new AvironException(
                        "Failed to kill the process " + pid, ex);
            }
        }
    }

    public static void validateLinuxOrMacOSX(final String fnName) {
         if (!(OS.isLinux() || OS.isMacOSX())) {
             throw new AvironException(fnName + " is available for Linux and MacOS only!");
         }
    }

    private static String slurp(final InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(
                                        new InputStreamReader(
                                                is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }
 
    private static String slurp(final File f) throws IOException {
        try {
            return new String(Files.readAllBytes(f.toPath()), Charset.defaultCharset());
        }
        catch(Exception ex) {
            return "»»» Error reading data from file: " + f.getPath();
        }
    }

    private static ShellResult getShellResult(final Process proc) 
    throws IOException, InterruptedException {
        final int exitCode = proc.waitFor();

        final String stdout = slurp(proc.getInputStream());
        final String stderr = slurp(proc.getErrorStream());

        return new ShellResult(stdout, stderr, exitCode);
    }
}
