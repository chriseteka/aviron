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
package com.github.jlangch.aviron;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.QuarantineFileActionException;


public class QuarantineEvent {

    public QuarantineEvent(
            final File infectedFile,
            final List<String> virusList,
            final File quarantineFile,
            final QuarantineFileAction action,
            final QuarantineFileActionException ex
    ) {
        this.infectedFile = infectedFile;
        if (virusList != null) this.virusList.addAll(virusList);
        this.quarantineFile = quarantineFile;
        this.action = action;
        this.ex = ex;
    }


    public File getInfectedFile() {
        return infectedFile;
    }

    public List<String> getVirusList() {
        return Collections.unmodifiableList(virusList);
    }

    public File getQuarantineFile() {
        return quarantineFile;
    }

    public QuarantineFileAction getAction() {
        return action;
    }

    public QuarantineFileActionException getException() {
        return ex;
    }

    public boolean hasException() {
        return ex != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Infected File: ");
        sb.append(infectedFile);
        sb.append(System.lineSeparator());
        sb.append("Virus Signatures: ");
        sb.append(virusList.stream().collect(Collectors.joining(", ")));
        sb.append(System.lineSeparator());
        sb.append("Quarantine File: ");
        sb.append(quarantineFile);
        sb.append(System.lineSeparator());
        sb.append("Quarantine Action: ");
        sb.append(action);
        sb.append(System.lineSeparator());
        if (ex != null) {
            sb.append("Quarantine Exception: ");
            sb.append(ex.getMessage());
        }

        return sb.toString();
    }

    
    private final File infectedFile;
    private final List<String> virusList = new ArrayList<>();
    private final File quarantineFile;
    private final QuarantineFileAction action;
    private final QuarantineFileActionException ex;
}
