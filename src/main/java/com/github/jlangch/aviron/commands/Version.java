package com.github.jlangch.aviron.commands;


/**
 * Print program and database versions.
 */
public class Version extends Command<String> {

    public Version() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("VERSION", CommandFormat.NULL_CHAR);
    }

    @Override
    protected String parseResponse(final String responseString) {
        return responseString;
    }

}