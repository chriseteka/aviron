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

import java.util.Objects;

import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Limits CPU for the clamd daemon.
 * 
 * <p>Remembers the limit set for the clamd daemon and changes to a new
 * limit only if the last {pid, limit} differs from the new {pid, limit}. 
 * 
 * <p>The <code>ClamdCpuLimiter</code> allows you to dynamically limit the 
 * CPU usage of the clamd daemon across the 24 hours of day.
 * 
 * <p>
 * The <i>cpulimit</i> tool must be installed to control the CPU limit
 * of the <i>clamd</i> daemon:
 * 
 * <pre>
 * Alma Linux:         » dnf install cpulimit
 * MacOS (Homebrew):   » brew install cpulimit
 * </pre>
 */
public class ClamdCpuLimiter {

    public ClamdCpuLimiter() {
    }


    public synchronized Limit getLimit() {
        return lastSeen;
    }

    /**
     * Activates a CPU limit [0..LIMIT] on the <i>clamd</i> process
     * 
     * @param clamdPID a clamd pid
     * @param limit a percent value 0..LIMIT
     * 
     * @see ClamdAdmin#activateClamdCpuLimit(String,int)
     * @see ClamdCpuLimiter#deactivateClamdCpuLimit
     */
    public synchronized boolean activateClamdCpuLimit(
            final String clamdPID, 
            final int limit
    ) {
        if (StringUtils.isBlank(clamdPID)) {
            throw new IllegalArgumentException("No Clamd PID!");
        }

        if (limit < 0) {
            throw new IllegalArgumentException(
                    "A limit value must not be negative!");
        }

        final Limit process = new Limit(clamdPID, limit);
        
        if (Objects.equals(clamdPID, lastSeen.pid) && limit == lastSeen.limit) {
            return false;  // no change
        }
        else {
            lastSeen = process;
            ClamdAdmin.activateClamdCpuLimit(clamdPID, limit);
            return true;
        }
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process
     * 
     * @param clamdPID a clamd pid
     * 
     * @see ClamdAdmin#deactivateClamdCpuLimit
     * @see ClamdCpuLimiter#activateClamdCpuLimit(String,int)
     */
    public synchronized void deactivateClamdCpuLimit(
            final String clamdPID
    ) {
        if (StringUtils.isBlank(clamdPID)) {
            throw new IllegalArgumentException("No Clamd PID!");
        }

        lastSeen = new Limit(null, 100);
        ClamdAdmin.deactivateClamdCpuLimit(clamdPID);
    }


    public static class Limit {
        public Limit(final String pid, final int limit) {
            this.pid = pid;
            this.limit = limit;
        }

        public String getPid() {
            return pid;
        }

        public int getLimit() {
            return limit;
        }

        @Override
        public String toString() {
            return String.format("pid=%s, limit=%d", pid, limit);
        }

        private final String pid;
        private final int limit;
    }


    private Limit lastSeen = new Limit(null, 100);
}
