package com.github.jlangch.aviron.commands.mgmt;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;

/**
 * Check the server's state. It should reply with "PONG". 
 */
public class Ping extends Command<Boolean> {

    public Ping() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("PING", CommandFormat.NULL_CHAR);
    }

    @Override
    protected Boolean parseResponse(final String responseString) {
        return responseString.trim().equalsIgnoreCase("PONG");
    }

}