package com.github.jlangch.aviron.commands;


public class CommandDef {

    public CommandDef(final String commandString, final CommandFormat format) {
        this.commandString = commandString;
        this.format = format;
    }


    public String getCommandString() {
        return commandString;
    }

    public CommandFormat getFormat() {
        return format;
    }

    
    private final String commandString;
    private final CommandFormat format;
}
