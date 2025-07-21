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
import com.github.jlangch.aviron.commands.scan.ScanResult;
import com.github.jlangch.aviron.ex.QuarantineFileActionException;


public class Quarantine {
    
    public Quarantine(
           final QuarantineFileAction quarantineFileAction,
           final File quarantineDir,
           final Consumer<QuarantineEvent> listener
    ) {
        this.quarantineFileAction = quarantineFileAction;
        this.quarantineDir = quarantineDir;
        this.listener = listener == null ? (e) -> {} : listener;
        this.listenerSupplied = listener != null;
    }
 
    
    public QuarantineFileAction getQuarantineFileAction() {
    	return quarantineFileAction;
    }
 
    public File getQuarantineDir() {
    	return quarantineDir;
    }
 
    public boolean hasListener() {
    	return listenerSupplied;
    }
 
    
    public void handleQuarantineActions(final ScanResult result) {
        if (quarantineFileAction == QuarantineFileAction.NONE
            || result == null
            || result.isOK()
        ) {
            return;
        }

        result.getVirusFound()
              .entrySet()
              .stream()
              .forEach(e -> processQuarantineAction(
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
                        || key.equals(KEY_QUARANTINE_ACTION)
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

    private void processQuarantineAction(final File file, final List<String> virusList) {
        if (!file.isFile() || !file.canRead()) {
            return;
        }

        synchronized (this) {
            File destFile = null;
            File destInfoFile = null;

            try {
                destFile = makeUniqueQuarantineFileName(file);
                destInfoFile = makeUniqueQuarantineInfoFileName(destFile);

                if (quarantineFileAction == QuarantineFileAction.MOVE) {
                    Files.move(
                            file.toPath(), 
                            destFile.toPath(), 
                            StandardCopyOption.ATOMIC_MOVE);
                }
                else if (quarantineFileAction == QuarantineFileAction.COPY) {
                    Files.copy(
                            file.toPath(), 
                            destFile.toPath(), 
                            StandardCopyOption.COPY_ATTRIBUTES);
                }

                makeQuarantineInfoFile(destInfoFile, file, virusList, quarantineFileAction);

                try {
                    listener.accept(
                        new QuarantineEvent(
                                file,
                                virusList,
                                destFile,
                                quarantineFileAction,
                                null));
                }
                catch(RuntimeException e) { /* not interested in sink problems */ }
            }
            catch(Exception ex) {
                try {
                    listener.accept(
                            new QuarantineEvent(
                                    file,
                                    virusList,
                                    destFile,
                                    quarantineFileAction,
                                    new QuarantineFileActionException(
                                            "Failed to process quarantine action", 
                                            ex)));
                }
                catch(RuntimeException e) { /* not interested in sink problems */ }
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
            final List<String> virusList,
            final QuarantineFileAction action
    ) {
        final List<String> data = new ArrayList<>();
        data.add(KEY_INFECTED_FILE + "=" + infectedFile.getPath());
        data.add(KEY_VIRUS_LIST + "=" + virusList.stream().collect(Collectors.joining(",")));
        data.add(KEY_QUARANTINE_ACTION + "=" + action.name());      
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


    public static String KEY_INFECTED_FILE     =  "infected-file";
    public static String KEY_VIRUS_LIST        =  "virus-list";
    public static String KEY_QUARANTINE_ACTION =  "quarantine-action";
    public static String KEY_CREATED_AT        =  "created-at";

    private final QuarantineFileAction quarantineFileAction;
    private final File quarantineDir;
    private final Consumer<QuarantineEvent> listener;
    private final boolean listenerSupplied;
}
