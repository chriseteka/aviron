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
package com.github.jlangch.aviron.examples;

import static com.github.jlangch.aviron.util.DemoUtil.printf;
import static com.github.jlangch.aviron.util.DemoUtil.printfln;
import static com.github.jlangch.aviron.util.DemoUtil.sleep;

import java.nio.file.Path;

import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchTerminationEvent;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.OS;


public class FileWatcherExample {

    public static void main(String[] args) {
        try {
            new FileWatcherExample().run();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() throws Exception {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Path mainDir = demoFS.getFilestoreDir().toPath();
            final boolean registerAllSubDirs = true;

            try(final IFileWatcher fw = createPlatformFileWatcher(mainDir, registerAllSubDirs)) {
                fw.setFileListener(this::onFileEvent);
                fw.setErrorListener(this::onErrorEvent);
                fw.setTerminationListener(this::onTerminationEvent);

                fw.start();

                printf("Ready to watch «%s»%n%n", mainDir);

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!

                demoFS.touchFilestoreFile("000", "test1.data");      // created
                sleep(1000);

                demoFS.appendToFilestoreFile("000", "test1.data");   // modified
                sleep(1000);

                demoFS.deleteFilestoreFile("000", "test1.data");     // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3000);
            }

            // wait to receive the termination event
            sleep(1000);
        }
    }


    private IFileWatcher createPlatformFileWatcher(
            final Path mainDir, 
            final boolean registerAllSubDirs
    ) {
        if (OS.isLinux()) {
            return new FileWatcher_JavaWatchService(mainDir, registerAllSubDirs);
        }
        else if (OS.isMacOSX()) {
            return new FileWatcher_FsWatch(
                         mainDir,
                         registerAllSubDirs,
                         null, // default fswatch monitor
                         FileWatcher_FsWatch.HOMEBREW_FSWATCH_PROGRAM);
        }
        else {
            throw new FileWatcherException(
                    "FileWatcher is not supported on platforms other than Linux/MacOS!");
        }
    }

    private void onFileEvent(final FileWatchFileEvent event) {
        if (event.isFile()) {
            printfln("File Event: %-8s %s", event.getType(), event.getPath());
        }
        else if (event.isDir()) {
            printfln("Dir Event:  %-8s %s", event.getType(), event.getPath());
        }
    }

    private void onErrorEvent(final FileWatchErrorEvent event) {
        printfln("Error:      %s %s", event.getPath(), event.getException().getMessage());
    }

    private void onTerminationEvent(final FileWatchTerminationEvent event) {
        printfln("Terminated: %s", event.getPath());
    }
}
