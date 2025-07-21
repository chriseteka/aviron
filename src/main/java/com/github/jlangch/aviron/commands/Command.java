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
