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

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jlangch.aviron.Client;


public class RealtimeScanner {

    public RealtimeScanner(
           final Client client,
           final File fileWatcherWAL,
           final Path dir
    ) {
        this.client = client;
        this.fileWatcherWAL = fileWatcherWAL;
        this.dir = dir;
    }


    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            // start realtime scanner

        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            // stop realtime scanner

        }
    }

    public void clearWAL() {
    }


    private static final AtomicBoolean running = new AtomicBoolean(false);

    private final Client client;
    private final File fileWatcherWAL;
    private final Path dir;
}
