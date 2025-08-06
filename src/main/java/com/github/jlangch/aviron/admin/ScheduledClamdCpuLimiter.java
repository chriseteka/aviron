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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.jlangch.aviron.util.service.Service;


/**
 * A service scheduler that updates the clamd daemon CPU limit in
 * regular intervals
 */
public class ScheduledClamdCpuLimiter extends Service {

    public ScheduledClamdCpuLimiter(
            final ClamdCpuLimiter limiter,
            final String clamdPID,
            final long initialDelay,
            final long period,
            final TimeUnit unit
    ) {
        this.limiter = limiter;
        this.clamdPID = clamdPID;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.es = Executors.newScheduledThreadPool(1);

        if (unit.toSeconds(period) < 60) {
            throw new IllegalArgumentException(
                    "The specified scheduler period must not be less than 60s");
        }
    }


    protected String name() {
        return "ScheduledClamdCpuLimiter";
    }

    protected void onStart() {
        final Runnable updateCpuLimitTask = () -> updateCpuLimit(limiter, clamdPID);

        es.scheduleAtFixedRate(updateCpuLimitTask, initialDelay, period, unit);
    }

    protected void onClose() {
        try {
            es.shutdown();
        }
        catch(Exception ex) {
            // skipped
        }
    }

    private void updateCpuLimit(final ClamdCpuLimiter limiter, final String clamdPID) {
        limiter.activateClamdCpuLimit(clamdPID);
    }


    private final ClamdCpuLimiter limiter;
    private final String clamdPID;
    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;

    private final ScheduledExecutorService es;
}
