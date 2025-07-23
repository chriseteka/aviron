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
package com.github.jlangch.aviron.quarantine;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.platform.commons.util.StringUtils;

import com.github.jlangch.aviron.commands.scan.ScanResult;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.ex.QuarantineException;
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


    public boolean isActive() {
        return quarantineFileAction != QuarantineFileAction.NONE;
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
    
    public List<QuarantineFile> listQuarantineFiles() {
        try {
            return Files.list(quarantineDir.toPath())
                        .map(p -> p.toFile().getPath())
                        .filter(f -> !f.endsWith(".virus"))
                        .map(f -> new File(f + ".virus"))
                        .map(f -> QuarantineFile.from(f))
                        .collect(Collectors.toList());
        }
        catch(Exception ex) {
            throw new QuarantineException("Failed to list quarantine files", ex);
        }
    }
    
    public void removeQuarantineFile(final QuarantineFile file) {
        if (file == null) {
            throw new IllegalArgumentException("A 'file' must not be null!");
        }

        final String filename = file.getQuarantineFileName();
        
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException(
                    "The field 'quarantineFileName' in the 'file' must not be empty!");
        }

        final File qFile = new File(quarantineDir, filename);
        final File qInfoFile = new File(quarantineDir, filename + ".virus");

        final boolean qFileDeleted = qFile.delete();      
        final boolean qInfoFileDeleted = qInfoFile.delete();

        if (!qFileDeleted && !qInfoFileDeleted) {
            throw new QuarantineException(
                    "The quarantine file «" + filename + "» could not be deleted");
        }
        else if (!qFileDeleted || !qInfoFileDeleted) {
            throw new QuarantineException(
                    "The quarantine file «" + filename + "» could not be completely deleted");
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
                    // Check if the same quarantined file already to prevent adding
                    // the infected file upon repeated scans over and over again!
                    if (existsAlready(file, virusList)) {
                        return;
                    }
                    
                    Files.copy(
                            file.toPath(), 
                            destFile.toPath(), 
                            StandardCopyOption.COPY_ATTRIBUTES);
                }

                makeQuarantineInfoFile(destFile, destInfoFile, file, virusList, quarantineFileAction);

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
            final File quarantineFile,
            final File quarantineInfoFile,
            final File infectedFile,
            final List<String> virusList,
            final QuarantineFileAction action
    ) {
        try {
              final QuarantineFile qf = new QuarantineFile(
                                                              quarantineFile.getName(),
                                                          infectedFile,
                                                          virusList,
                                                          action,
                                                          LocalDateTime.now(),
                                                          action == QuarantineFileAction.MOVE
                                                            ? hashFile(quarantineFile)
                                                            : hashFile(infectedFile));

            Files.write(
                  quarantineInfoFile.toPath(), 
                  qf.format().getBytes(Charset.defaultCharset()),
                StandardOpenOption.CREATE);
        }
        catch(Exception ex) {
            throw new QuarantineFileActionException(
                    "Failed to create quarantine info file name for infected file "
                    + "«" + infectedFile + "»!",
                    ex);
        }
    }

    private boolean existsAlready(final File infectedFile, final List<String> virusList) {
        return listQuarantineFiles()
                    .stream()
                    .anyMatch(f -> f.getInfectedFile().equals(infectedFile)
                                       && f.getHash().equals(hashFile(infectedFile)));
    }
    
    private String hashFile(final File file) {
          return Hashes.hashFile(HASH_ALGO, HASH_SALT, file);
    }


    // MD5 is fast and okay to compute hashes for the purpose of detecting
    // duplicate files only!
    private final static String HASH_ALGO = "MD5";
    private final static String HASH_SALT = "ClamAV Aviron";
                
    private final QuarantineFileAction quarantineFileAction;
    private final File quarantineDir;
    private final Consumer<QuarantineEvent> listener;
    private final boolean listenerSupplied;
}
