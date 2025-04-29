package com.github.jlangch.aviron.commands.scan;

import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;


/**
 * Scan a file or a directory (recursively) with archive support enabled 
 * (if not disabled in clamd.conf). A full path is required. 
 * 
 * <p>Stops scanning when a virus is found
 */
public class Scan extends ScanCommand {

    public Scan(final String path) {
        this.path = path;
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("SCAN", CommandFormat.NULL_CHAR);
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