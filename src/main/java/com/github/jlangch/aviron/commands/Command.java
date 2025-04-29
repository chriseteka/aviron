package com.github.jlangch.aviron.commands;

import com.github.jlangch.aviron.ex.UnknownCommandException;
import com.github.jlangch.aviron.server.ServerIO;

public abstract class Command<T> {

    public abstract CommandDef getCommandDef();

    public final String getCommandString() {
        return getCommandDef().getCommandString();
    }

    public final CommandFormat getFormat() {
        return getCommandDef().getFormat();
    }

    public T send(final ServerIO server) {
        final String rawResponse = server.sendCommandAndReturnResponse(rawCommand());

        final String response = removeResponseTerminator(rawResponse);
        if ("UNKNOWN COMMAND".equals(response)) {
             throw new UnknownCommandException(getCommandString());
        }

        return parseResponse(response);
    }

    protected String rawCommand() {
        return String.format(
                "%s%s%s", 
                getFormat().getPrefix(), 
                getCommandString(), 
                getFormat().getTerminator());
    }

    protected abstract T parseResponse(String responseString);

    protected String removeResponseTerminator(final String response) {
        final int index = response.lastIndexOf(getFormat().getTerminator());
        return index >= 0 ? response.substring(0, index) : response;
    }
}
