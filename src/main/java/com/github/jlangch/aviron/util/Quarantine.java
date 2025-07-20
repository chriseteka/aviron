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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.QuarantineEvent;
import com.github.jlangch.aviron.QuarantineFileAction;
import com.github.jlangch.aviron.QuarantineFileActionException;
import com.github.jlangch.aviron.commands.scan.ScanResult;


public class Quarantine {
    
    public Quarantine(
           final QuarantineFileAction quarantineFileAction,
           final File quarantineDir,
           final Consumer<QuarantineEvent> listener
    ) {
        this.quarantineFileAction = quarantineFileAction;
        this.quarantineDir = quarantineDir;
        this.listener = listener;
    }
    
    public void handleQuarantineActions(final ScanResult result) {
        if (result.isOK()) {
            return;
        }

        result.getVirusFound()
              .entrySet()
              .stream()
              .forEach(e -> runQuarantineAction(
                                  new File(e.getKey()),
                                  e.getValue()));
    }

    public static Map<String,String> parseQuarantineInfoFile(final File infoFile) {
        if (infoFile == null) {
            throw new IllegalArgumentException("An 'infoFile' must not be null!");
        }

        try {
            final List<String> lines = Files.lines(infoFile.toPath())
                                            .collect(Collectors.toList());
 
            final Map<String,String> data = new HashMap<>();

            for(String line : lines) {
                final int pos = line.indexOf('=');
                if (pos > 0) {
                    final String key = line.substring(0, pos);
                    final String value = line.substring(pos+1);

                    if (key.equals(KEY_INFECTED_FILE)
                        || key.equals(KEY_VIRUS_LIST)
                        || key.equals(KEY_CREATED_AT)
                    ) {
                        data.put(key, value);
                    }
                }
            }

            return data;
        }
        catch(Exception ex) {
            throw new QuarantineFileActionException(
                    "Failed to parser virus info file " + "«" + infoFile + "». ",
                    ex);
        }
    }

    private void runQuarantineAction(final File file, final List<String> virusList) {
        if (!file.isFile()) {
            return;
        }

        if (!file.canRead()) {
            return;
        }

        // note: listener is not called when these 2 actions fail!
        final File destFile = makeUniqueQuarantineFileName(file);
        final File destInfoFile = makeUniqueQuarantineInfoFileName(destFile);

        if (quarantineFileAction == QuarantineFileAction.MOVE) {
            try {
                Files.move(
                        file.toPath(), 
                        destFile.toPath(), 
                        StandardCopyOption.ATOMIC_MOVE);

                makeQuarantineInfoFile(destInfoFile, file, virusList);

                try {
                    listener.accept(
                        new QuarantineEvent(
                                file,
                                virusList,
                                destFile,
                                QuarantineFileAction.MOVE,
                                null));
                }
                catch(RuntimeException e) { /* not interest in sink problems */ }
            }
            catch(Exception ex) {
                try {
                    listener.accept(
                            new QuarantineEvent(
                                    file,
                                    virusList,
                                    destFile,
                                    QuarantineFileAction.MOVE,
                                    new QuarantineFileActionException("", ex)));
                }
                catch(RuntimeException e) { /* not interest in sink problems */ }
            }
        }

        else if (quarantineFileAction == QuarantineFileAction.COPY) {
            try {
                Files.copy(
                        file.toPath(), 
                        destFile.toPath(), 
                        StandardCopyOption.COPY_ATTRIBUTES);

                makeQuarantineInfoFile(destInfoFile, file, virusList);

                if (listener != null) {
                    try {
                        listener.accept(
                                new QuarantineEvent(
                                        file,
                                        virusList,
                                        destFile,
                                        QuarantineFileAction.COPY,
                                        null));
                    }
                    catch(RuntimeException e) { /* not interest in sink problems */ }
                }
            }
            catch(Exception ex) {
                try {
                    listener.accept(
                            new QuarantineEvent(
                                    file,
                                    virusList,
                                    destFile,
                                    QuarantineFileAction.COPY,
                                    new QuarantineFileActionException("", ex)));
                }
                catch(RuntimeException e) { /* not interest in sink problems */ }
            }
        }
    }
 
    private File makeUniqueQuarantineFileName(final File file) {
        final String name = file.getName();

        File destFile = new File(quarantineDir, name);
        if (!destFile.isFile()) {
            return destFile;
        }

        for(int ii=1; ii<=100; ii++) {
            destFile = new File(quarantineDir, name + "." + ii);
            if (!destFile.isFile()) {
                return destFile;
            }
        }

        throw new QuarantineFileActionException(
                "Failed to find unique quarantine file name for infected file "
                + "«" + file + "». "
                + "Infected file not moved/copied to quarantine dir!");
    }

    private File makeUniqueQuarantineInfoFileName(final File file) {
        return new File(file.getParentFile(), file.getName() + ".virus");
    }

    private void makeQuarantineInfoFile(
            final File infoFile,
            final File infectedFile,
            final List<String> virusList
    ) {
        final List<String> data = new ArrayList<>();
        data.add(KEY_INFECTED_FILE + "=" + infectedFile.getPath());
        data.add(KEY_VIRUS_LIST + "=" + virusList.stream().collect(Collectors.joining(",")));
        data.add(KEY_CREATED_AT + "=" + LocalDateTime.now().toString());

        try {
            Files.write(
                infoFile.toPath(), 
                data,
                StandardOpenOption.CREATE);
        }
        catch(IOException ex) {
            throw new QuarantineFileActionException(
                    "Failed to create quarantine info file name for infected file "
                    + "«" + infectedFile + "»!",
                    ex);
        }
    }


    public static String KEY_INFECTED_FILE =  "infected-file";
    public static String KEY_VIRUS_LIST    =  "virus-list";
    public static String KEY_CREATED_AT    =  "created-at";

    private final QuarantineFileAction quarantineFileAction;
    private final File quarantineDir;
    final Consumer<QuarantineEvent> listener;
}
