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

import java.util.List;


public class ShellResult {

    public ShellResult(
            final String stdout,
            final String stderr,
            final int exitCode
    ) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }


    public String getStdout() {
        return stdout;
    }

    public List<String> getStdoutLines() {
        return StringUtils.splitIntoLines(stdout);
    }

    public String getStderr() {
        return stderr;
    }

    public List<String> getStderrLines() {
        return StringUtils.splitIntoLines(stderr);
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isZeroExitCode() {
        return exitCode == 0;
    }


    @Override
    public String toString() {
        final String err = StringUtils.trimToNull(stderr);
        final String out = StringUtils.trimToNull(stdout);

        final StringBuilder sb = new StringBuilder();

        sb.append("Exit code: " + exitCode + "\n");
        
        if (out != null) {
            sb.append("[stdout]\n");
            sb.append(out);
        }

        if (err != null) {
            sb.append("\n[stderr]\n");
            sb.append(err);
        }

        return sb.toString();
    }


    private final String stdout;
    private final String stderr;
    private final int exitCode;
}
