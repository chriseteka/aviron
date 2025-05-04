/*                 _                 
 *       /\       (_)            
 *      /  \__   ___ _ __ ___  _ __  
 *     / /\ \ \ / / | '__/ _ \| '_ \ 
 *    / ____ \ V /| | | | (_) | | | |
 *   /_/    \_\_/ |_|_|  \___/|_| |_|
 *
 *
 * Copyright 2025 Aviron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jlangch.aviron.commands.mgmt;

import java.util.Arrays;
import java.util.List;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;
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