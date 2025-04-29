package com.github.jlangch.aviron.commands.scan;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        return isOK()
                ? "ScanResult.OK"
                : String.format("ScanResult.VirusFound{foundViruses=%s}", foundViruses);
    }


    private final Map<String, List<String>> foundViruses = new HashMap<>();
}