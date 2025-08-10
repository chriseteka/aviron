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
package com.github.jlangch.aviron.processor;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import com.github.jlangch.aviron.events.FilestoreScanEvent;
import com.github.jlangch.aviron.util.IDirCycler;
import com.github.jlangch.aviron.util.service.Service;


/**
 * Filestore directory processor
 */
public class FilestoreDirProcessor extends Service {

    public FilestoreDirProcessor(
           final IDirCycler cycler,
           final int sleepTimeSecondsOnIdle,
           final int sleepTimeSecondsAtRollover,
           final Consumer<FilestoreScanEvent> scanListener
    ) {
        if (cycler == null) {
            throw new IllegalArgumentException("A 'cycler' must not be null!");
        }
        if (scanListener == null) {
            throw new IllegalArgumentException("A 'scanListener' must not be null!");
        }

        this.cycler = cycler;
        this.sleepTimeSecondsOnIdle = Math.max(1, sleepTimeSecondsOnIdle);
        this.sleepTimeSecondsAtRollover = Math.max(1, sleepTimeSecondsAtRollover);
        this.scanListener = scanListener;
    }


    @Override
    protected String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void onStart() {
        cycler.refresh();

        startServiceThread(createWorker());
    }

    @Override
    protected void onClose() throws IOException{
    }

    private Runnable createWorker() {
        return () -> {
            enteredRunningState();

            while (isInRunningState()) {
                try {
                    if (cycler.isEmpty()) {
                        Thread.sleep(sleepTimeSecondsOnIdle * 1_000);
                        continue;
                    }

                    final File dir = cycler.nextDir();
                    fireEvent(new FilestoreScanEvent(dir.toPath()));

                    if (cycler.isLast()) {
                        Thread.sleep(sleepTimeSecondsAtRollover * 1_000);
                        continue;
                    }
                }
                catch(Exception ex) {
                    // prevent thread spinning in fatal error conditions
                    sleep(5_000);
                }
            }
        };
    }

    private void fireEvent(final FilestoreScanEvent event) {
        safeRun(() -> scanListener.accept(event));
    }

    private void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


    private final IDirCycler cycler;
    private final Consumer<FilestoreScanEvent> scanListener;
    private final int sleepTimeSecondsOnIdle;
    private final int sleepTimeSecondsAtRollover;
}
