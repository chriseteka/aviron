package com.github.jlangch.aviron.commands.scan;

import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;


/**
 * Scan file in a standard way or scan directory (recursively) using multiple 
 * threads (to make the scanning faster on SMP machines). 
 */
public class MultiScan extends ScanCommand {

    public MultiScan(String path) {
        this.path = path;
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("MULTISCAN", CommandFormat.NEW_LINE);
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