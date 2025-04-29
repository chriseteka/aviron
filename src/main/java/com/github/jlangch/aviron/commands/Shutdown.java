package com.github.jlangch.aviron.commands;


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