package com.github.jlangch.aviron.commands.scan;

import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;

/**
 * Scan file or directory (recursively) with archive support enabled and don't 
 * stop the scanning when a virus is found.
 */
public class ContScan extends ScanCommand {

    public ContScan(String path) {
        this.path = path;
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("CONTSCAN", CommandFormat.NEW_LINE);
    }

    @Override
    protected String rawCommand() {
        return String.format(
                "%s%s %s%s", 
                getFormat().getPrefix(), 
                getCommandString(), 
                path, 
                getFormat().getTerminator());
    }


    private final String path;
}