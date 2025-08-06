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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.jlangch.aviron.impl.util.StringUtils;
import com.github.jlangch.aviron.util.service.Service;


/**
 * A service scheduler that updates the clamd daemon CPU limit in
 * regular intervals
 */
public class ScheduledClamdCpuLimiter extends Service {

    public ScheduledClamdCpuLimiter(
            final String clamdPid,
            final ClamdCpuLimiter limiter,
            final long initialDelay,
            final long period,
            final TimeUnit unit
    ) {
        this.clamdPid = clamdPid;
        this.clamdPidFile = null;
        this.limiter = limiter;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.es = Executors.newScheduledThreadPool(1);

        if (unit.toSeconds(period) < 60) {
            throw new IllegalArgumentException(
                    "The specified scheduler period must not be less than 60s");
        }
    }

    public ScheduledClamdCpuLimiter(
            final File clamdPidFile,
            final ClamdCpuLimiter limiter,
            final long initialDelay,
            final long period,
            final TimeUnit unit
    ) {
        this.clamdPid = null;
        this.clamdPidFile = clamdPidFile;
        this.limiter = limiter;
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
        final Runnable updateCpuLimitTask = () -> updateCpuLimit(limiter);

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

    private void updateCpuLimit(
            final ClamdCpuLimiter limiter
    ) {
        final String pid = getClamdPID();
        if (StringUtils.isNotBlank(pid)) {
            limiter.activateClamdCpuLimit(pid);
        }
    }

    private String getClamdPID() {
        final String pid = clamdPidFile != null
                            ? ClamdAdmin.loadClamdPID(clamdPidFile)
                            : clamdPid;
        return ClamdAdmin.isProcessAlive(pid) ? pid : null;
    }


    private final ClamdCpuLimiter limiter;
    private final String clamdPid;
    private final File clamdPidFile;
    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;

    private final ScheduledExecutorService es;
}
