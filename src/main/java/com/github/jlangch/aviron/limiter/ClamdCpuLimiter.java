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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.github.jlangch.aviron.admin.ClamdAdmin;
import com.github.jlangch.aviron.events.ClamdCpuLimitChangeEvent;
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
 * 
 * <p>
 * Physically we do not go below MIN_SCAN_LIMIT_PERCENT for the clamd daemon 
 * CPU limit! Otherwise the daemon is not reactive any more.
 * 
 * <p>
 * To declare a scan free time period use the limit from the CpuProfile 
 * and simply do not run scan events at all!
 * 
 * @see #MIN_SCAN_LIMIT_PERCENT
 */
public class ClamdCpuLimiter {

    public ClamdCpuLimiter(
            final ClamdPid clamdPid,
            final DynamicCpuLimit dynamicCpuLimit
    ) {
        this.clamdPid = clamdPid;
        this.dynamicCpuLimit = dynamicCpuLimit == null 
                                  ? new DynamicCpuLimit() 
                                  : dynamicCpuLimit;
    }

    /**
     * Register a Clamd CPU limit change listener.
     * 
     * @param listener a listener
     */
    public void setClamdCpuLimitChangeListener(
            final Consumer<ClamdCpuLimitChangeEvent> listener
    ) {
        limitChangeListener.set(listener);
    }

    /**
     * @return the associated clamd pid
     */
    public ClamdPid getClamdPid() {
        return clamdPid;
    }

    /**
     * Enable/disable mocking mode.
     * 
     * <p>If mocking is enabled modifying the physical clamd daemon CPU limit
     * will be skipped. Apart from this ClamdCpuLimiter is behaving as in non
     * mocking mode. 
     * 
     * @param mock enable/disable mocking mode
     */
    public void mocking(final boolean mock) {
        mocking.set(mock);
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
     * @param limit a percent value 0..LIMIT
     * @return Returns <code>true</code> if the limit has been changed to the
     *         new value else <code>false</code> if the limit was already at
     *         the desired value.
     * 
     * @see ClamdCpuLimiter#activateClamdCpuLimit()
     * @see ClamdCpuLimiter#deactivateClamdCpuLimit()
     */
    public synchronized boolean activateClamdCpuLimit(
            final int limit
    ) {
        if (limit < 0) {
            throw new IllegalArgumentException(
                    "A limit value must not be negative!");
        }

        if (!mocking.get()) {
            // get the clamd daemon pid
            final String pid = clamdPid.getPid();

            if (!StringUtils.isBlank(pid)) {
                final Limit newLimit = new Limit(pid, limit);

                if (Objects.equals(pid, lastSeen.pid) && limit == lastSeen.limit) {
                    return false;  // no change
                }
                else {
                    final ClamdCpuLimitChangeEvent event = new ClamdCpuLimitChangeEvent(
                                                                   lastSeen.limit, limit);

                    lastSeen = newLimit;
                    
                    // physically we do not go below MIN_SCAN_LIMIT_PERCENT for the
                    // clamd daemon CPU limit! Otherwise the daemon is not reactive any
                    // more.
                    //
                    // To declare a scan free time period use the limit from the 
                    // CpuProfile and simply do not run scan events at all!
                    ClamdAdmin.activateClamdCpuLimit(
                        pid, 
                        Math.max(MIN_SCAN_LIMIT_PERCENT, limit));

                    fireEvent(event);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Activates the current limit obtained from the dynamic limit computation 
     * on the <i>clamd</i> process.
     * 
     * @return Returns <code>true</code> if the limit has been changed to the
     *         new value else <code>false</code> if the limit was already at
     *         the desired value.
     * 
     * @see ClamdCpuLimiter#activateClamdCpuLimit(int)
     * @see ClamdCpuLimiter#deactivateClamdCpuLimit()
     */
    public synchronized boolean activateClamdCpuLimit() {
        final int limit = dynamicCpuLimit.computeCpuLimit();

        return activateClamdCpuLimit(limit);
    }

    /**
     * Deactivates the CPU limit on the <i>clamd</i> process
     * 
     * @see ClamdCpuLimiter#activateClamdCpuLimit()
     * @see ClamdCpuLimiter#activateClamdCpuLimit(int)
     */
    public synchronized void deactivateClamdCpuLimit() {
        if (!mocking.get()) {
            // get the clamd daemon pid
            final String pid = clamdPid.getPid();

            if (!StringUtils.isBlank(pid)) {
               final ClamdCpuLimitChangeEvent event = new ClamdCpuLimitChangeEvent(
                                                               lastSeen.limit, 100);

                lastSeen = new Limit(null, 100);
                
                ClamdAdmin.deactivateClamdCpuLimit(pid);
                fireEvent(event);
            }
        }
    }

    public String formatProfilesAsTableByHour() {
        return dynamicCpuLimit.formatProfilesAsTableByHour();
    };

    @Override
    public String toString() {
        return dynamicCpuLimit.toString();
    };



    private void fireEvent(final ClamdCpuLimitChangeEvent event) {
        final Consumer<ClamdCpuLimitChangeEvent> listener = limitChangeListener.get();
        if (listener != null) {
            safeRun(() -> listener.accept(event));
        }
    }

    private static void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


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


    public static final int MIN_SCAN_LIMIT_PERCENT = 20;

    private Limit lastSeen = new Limit(null, -1);

    private final ClamdPid clamdPid;
    private final AtomicBoolean mocking = new AtomicBoolean(false);
    private final AtomicReference<Consumer<ClamdCpuLimitChangeEvent>> limitChangeListener = new AtomicReference<>();
    private final DynamicCpuLimit dynamicCpuLimit;
}
