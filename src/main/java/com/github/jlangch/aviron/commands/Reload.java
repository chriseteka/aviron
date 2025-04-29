package com.github.jlangch.aviron.commands;

import com.github.jlangch.aviron.ex.InvalidResponseException;

/**
 * Reload the virus databases. 
 */
public class Reload extends Command<Void> {

    public Reload() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("RELOAD", CommandFormat.NULL_CHAR);
    }

    @Override
    protected Void parseResponse(final String responseString) {
        if (!"RELOADING".equals(responseString)) {
            throw new InvalidResponseException(responseString);
        }

        return null;
    }

}