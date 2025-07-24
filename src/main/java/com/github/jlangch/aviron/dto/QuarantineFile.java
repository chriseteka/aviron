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
package com.github.jlangch.aviron.dto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.ex.QuarantineException;
import com.github.jlangch.aviron.util.StringUtils;


public class QuarantineFile {

    public QuarantineFile(
            final String quarantineFileName,
            final File infectedFile,
            final List<String> virusList,
            final QuarantineFileAction action,
            final LocalDateTime quarantinedAt,
            final String hash
    ) {
        this.quarantineFileName = quarantineFileName;
        this.infectedFile = infectedFile;
        this.virusList.addAll(virusList);
        this.action = action;
        this.quarantinedAt = quarantinedAt;
        this.hash = hash;
    }

    public static QuarantineFile from(final File quarantineInfoFile) {
        if (quarantineInfoFile == null) {
            throw new IllegalArgumentException("An 'quarantineInfoFile' must not be null!");
        }

        if (!quarantineInfoFile.getPath().endsWith(".virus")) {
            throw new QuarantineException(
                    "The file «" + quarantineInfoFile + "» is not a quarantine info file!");
        }

        try {
            final String data = new String(
                                        Files.readAllBytes(quarantineInfoFile.toPath()),
                                        Charset.defaultCharset());

            return from(quarantineInfoFile.getName(), data);
        }
        catch(IOException ex) {
            throw new QuarantineException(
                    "Failed read quarantine info file «" + quarantineInfoFile + "»!");
        }
    }

    public static QuarantineFile from(
            final String quarantineInfoFileName,
            final String quarantineInfoFileData
    ) {
        try {
            String quarantineFile = StringUtils.stripEnd(quarantineInfoFileName, ".virus");
            File infectedFile = null;
            List<String> virusList = new ArrayList<>();
            QuarantineFileAction action = null;
            LocalDateTime createdAt = null;
            String hash = null;

            for(String line : StringUtils.splitIntoLines(quarantineInfoFileData)) {
                final int pos = line.indexOf('=');
                if (pos > 0) {
                    final String key = line.substring(0, pos);
                    final String value = line.substring(pos+1);

                    if (key.equals(KEY_INFECTED_FILE)) {
                        infectedFile = new File(value);
                    }
                    else if (key.equals(KEY_VIRUS_LIST)) {
                        virusList = Arrays.asList(value.split(","));
                    }
                    else if (key.equals(KEY_QUARANTINE_ACTION)) {
                        action = QuarantineFileAction.valueOf(value);
                    }
                    else if (key.equals(KEY_QUARANTINED_AT)) {
                        createdAt = LocalDateTime.parse(value);
                    }
                    else if (key.equals(KEY_HASH)) {
                        hash = value;
                    }
                 }
            }

            return new QuarantineFile(
                        quarantineFile,
                        infectedFile,
                        virusList,
                        action,
                        createdAt,
                        hash);
        }
        catch(Exception ex) {
            throw new QuarantineException(
                    "Failed to parse quarantine info file " + "«" + quarantineInfoFileName + "». ",
                    ex);
        }
    }


    public String getQuarantineFileName() {
        return quarantineFileName;
    }

    public File getInfectedFile() {
        return infectedFile;
    }

    public List<String> getVirusList() {
        return virusList;
    }

    public String getVirusListFormatted() {
        return virusList.stream().collect(Collectors.joining(","));
    }

    public QuarantineFileAction getAction() {
        return action;
    }

    public LocalDateTime getQuarantinedAt() {
        return quarantinedAt;
    }

    public String getHash() {
        return hash;
    }
    

    public String format() {
        final StringBuilder sb = new StringBuilder();

        sb.append(KEY_INFECTED_FILE);
        sb.append("=");
        sb.append(getInfectedFile());
        sb.append(System.lineSeparator());

        sb.append(KEY_VIRUS_LIST);
        sb.append("=");
        sb.append(getVirusListFormatted());
        sb.append(System.lineSeparator());

        sb.append(KEY_QUARANTINE_ACTION);
        sb.append("=");
        sb.append(getAction().name());
        sb.append(System.lineSeparator());

        sb.append(KEY_QUARANTINED_AT);
        sb.append("=");
        sb.append(getQuarantinedAt());
        sb.append(System.lineSeparator());

        sb.append(KEY_HASH);
        sb.append("=");
        sb.append(getHash());
        sb.append(System.lineSeparator());

        return sb.toString();
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Quarantine File Name: ");
        sb.append(getQuarantineFileName());
        sb.append(System.lineSeparator());

        sb.append("Infected File: ");
        sb.append(getInfectedFile());
        sb.append(System.lineSeparator());

        sb.append("Virus Signatures: ");
        sb.append(getVirusListFormatted());
        sb.append(System.lineSeparator());

        sb.append("Quarantine Action: ");
        sb.append(getAction());
        sb.append(System.lineSeparator());

        sb.append("Quarantined At: ");
        sb.append(getQuarantinedAt());
        sb.append(System.lineSeparator());

        sb.append("Hash: ");
        sb.append(getHash());

        return sb.toString();
    }


    private static String KEY_INFECTED_FILE     =  "infected-file";
    private static String KEY_VIRUS_LIST        =  "virus-list";
    private static String KEY_QUARANTINE_ACTION =  "quarantine-action";
    private static String KEY_QUARANTINED_AT    =  "quarantined-at";
    private static String KEY_HASH              =  "hash";

    private final String quarantineFileName;
    private final File infectedFile;
    private final List<String> virusList = new ArrayList<>();
    private final QuarantineFileAction action;
    private final LocalDateTime quarantinedAt;
    private final String hash;
}
