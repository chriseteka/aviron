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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ScanResult {

    private ScanResult(final Map<String, List<String>> foundViruses) {
        if (foundViruses != null) {
            this.foundViruses.putAll(foundViruses);
        }
    }

    public static ScanResult ok() {
        return new ScanResult(null);
    }

    public static ScanResult virusFound(final Map<String, List<String>> foundViruses) {
        return new ScanResult(foundViruses);
    }


    public boolean isOK() {
        return foundViruses.isEmpty();
    }
  
    public boolean hasVirus() {
        return !foundViruses.isEmpty();
    }

    public Map<String, List<String>> getVirusFound() {
        return Collections.unmodifiableMap(foundViruses);
    }
    
    public void mergeWith(final ScanResult other) {
        if (other == null) {
            return;
        }
        else {
            // merge found viruses
            other.foundViruses.forEach((k,v) -> {
                final List<String> vals = foundViruses.get(k);
                if (vals == null) {
                    foundViruses.put(k,v);
                } 
                else {
                    vals.addAll(v);
                }
            });
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        if (isOK()) {
            sb.append("ScanResult: OK");
        }
        else {
            sb.append("ScanResult: Virus found");
            for(Map.Entry<String, List<String>> e : foundViruses.entrySet()) {
                sb.append(System.lineSeparator());
                sb.append("File: ");
                sb.append(e.getKey());
                sb.append(System.lineSeparator());
                sb.append("Virus Signatures: ");
                sb.append(e.getValue().stream().collect(Collectors.joining(", ")));
            }
        }
        
        return sb.toString();
     }


    private final Map<String, List<String>> foundViruses = new HashMap<>();
}