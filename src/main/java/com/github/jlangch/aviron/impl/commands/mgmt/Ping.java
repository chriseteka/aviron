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
package com.github.jlangch.aviron.impl.commands.mgmt;

import com.github.jlangch.aviron.impl.commands.Command;
import com.github.jlangch.aviron.impl.commands.CommandDef;
import com.github.jlangch.aviron.impl.commands.CommandFormat;


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