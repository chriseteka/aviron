package com.github.jlangch.aviron.commands;


/**
 * It is mandatory to newline terminate this command, or prefix with n or z, 
 * it is recommended to only use the z prefix.
 *
 * <p>Replies with statistics about the scan queue, contents of scan queue, and 
 * memory usage. The exact reply format is subject to change in future releases. 
 */
public class Stats extends Command<String> {

    public Stats() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("STATS", CommandFormat.NEW_LINE);
    }

    @Override
    protected String parseResponse(final String responseString) {
        return responseString;
    }

}