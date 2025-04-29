package com.github.jlangch.aviron.commands;

import java.util.Arrays;
import java.util.List;

import com.github.jlangch.aviron.ex.InvalidResponseException;



/**
 * Print program and database versions, followed by "| COMMANDS:" and a 
 * space-delimited list of supported commands. Clamd &lt;0.95 will recognize 
 * this as the VERSION command, and reply only with their version, without 
 * the commands list. 
 */
public class VersionCommands extends Command<List<String>> {

    public VersionCommands() {
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("VERSIONCOMMANDS", CommandFormat.NEW_LINE);
    }

    @Override
    protected List<String> parseResponse(final String responseString) {
        final int commandsStartPos = responseString.indexOf(COMMANDS_START_TAG);
        if (commandsStartPos == -1) {
            throw new InvalidResponseException(responseString);
        }

        final String commandsPart = responseString.substring(commandsStartPos + COMMANDS_START_TAG.length())
        		                                  .trim();
        
        return Arrays.asList(commandsPart.split("\\s+"));
    }


    private static final String COMMANDS_START_TAG = "| COMMANDS:";
}