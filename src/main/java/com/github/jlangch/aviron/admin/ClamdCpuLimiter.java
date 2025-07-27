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

import java.time.LocalDateTime;
import java.util.Objects;

import com.github.jlangch.aviron.impl.util.StringUtils;


/**
 * Limits the CPU for the clamd daemon.
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
        this(null);
    }

    public ClamdCpuLimiter(final DynamicCpuLimit dynamicCpuLimit) {
        this.dynamicCpuLimit = dynamicCpuLimit == null 
                                  ? new DynamicCpuLimit() 
                                  : dynamicCpuLimit;
    }


    /**
     * Returns the limit for a given timestamp. If the timestamp is <code>null</code>
     * returns the limit for <i>now</i>.
     * 
     * @param timestamp a timestamp
     * @return the cpu limit
     */
    public int getLimitForTimestamp(final LocalDateTime timestamp) {
        return dynamicCpuLimit.computeCpuLimit(timestamp);
    }

    /**
     * Returns the CPU limit last activated on the clamd daemon
     * 
     * @return the cpu limit
     */
    public synchronized int getLastSeenLimit() {
        return lastSeen.limit;
    }

    /**
     * Activates a CPU limit on the <i>clamd</i> process. 
     * 
     * <p>The limit must be in the range  [0..LIMIT] depending on the number 
     * of logical processors: 
     * <ul>
     *  <li>on a 8 core <i>MacBook Air</i> LIMIT is 800%</li>
     *  <li>on a <i>Intel</i> single core with 2 hyperthreads LIMIT is 200%</li>
     * </ul>
     * 
     * 
     * @param clamdPID a clamd pid
     * @param limit a percent value 0..LIMIT
     * @return Returns <code>true</code> if the limit has been changed to the
     *         new value else <code>false</code> if the limit was already at
     *         the desired value.
     * 
     * @see ClamdAdmin#activateClamdCpuLimit(String,int)
     * @see ClamdCpuLimiter#activateClamdCpuLimit(String)
     * @see ClamdCpuLimiter#deactivateClamdCpuLimit(String)
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

        final Limit newLimit = new Limit(clamdPID, limit);

        if (Objects.equals(clamdPID, lastSeen.pid) && limit == lastSeen.limit) {
            return false;  // no change
        }
        else {
            lastSeen = newLimit;
            ClamdAdmin.deactivateClamdCpuLimit(clamdPID);
            if (limit != 100) {
            	ClamdAdmin.activateClamdCpuLimit(clamdPID, limit);
            }
            return true;
        }
    }

    /**
     * Activates the current limit obtained from the dynamic limit computation 
     * on the <i>clamd</i> process.
     * 
     * @param clamdPID a clamd pid
     * 
     * @see ClamdAdmin#activateClamdCpuLimit(String,int)
     * @see ClamdCpuLimiter#activateClamdCpuLimit(String,int)
     * @see ClamdCpuLimiter#deactivateClamdCpuLimit(String)
     */
    public synchronized void activateClamdCpuLimit(
            final String clamdPID
    ) {
        if (StringUtils.isBlank(clamdPID)) {
            throw new IllegalArgumentException("No Clamd PID!");
        }

        final int limit = dynamicCpuLimit.computeCpuLimit();

        activateClamdCpuLimit(clamdPID, limit);
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process
     * 
     * @param clamdPID a clamd pid
     * 
     * @see ClamdAdmin#deactivateClamdCpuLimit(String)
     * @see ClamdCpuLimiter#activateClamdCpuLimit(String,int)
     * @see ClamdCpuLimiter#activateClamdCpuLimit(String)
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


    @Override
    public String toString() {
        return dynamicCpuLimit.toString();
    };


    private static class Limit {
        public Limit(final String pid, final int limit) {
            this.pid = pid;
            this.limit = limit;
            this.ts = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("pid=%s, limit=%d, ts=%s", pid, limit, ts.toString());
        }

        private final String pid;
        private final int limit;
        private final LocalDateTime ts;
    }



    private Limit lastSeen = new Limit(null, 100);

    private final DynamicCpuLimit dynamicCpuLimit;
}
