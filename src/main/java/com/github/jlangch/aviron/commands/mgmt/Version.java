package com.github.jlangch.aviron.commands.mgmt;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;

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