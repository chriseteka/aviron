package com.github.jlangch.aviron.commands.mgmt;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;

/**
 * Perform a clean exit. 
 */
public class Shutdown extends Command<Void> {

    public Shutdown() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("SHUTDOWN", CommandFormat.NULL_CHAR);
    }

    @Override
    protected Void parseResponse(final String responseString) {
        return null;
    }

}