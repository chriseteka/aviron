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
package com.github.jlangch.aviron.impl.commands.scan;

import com.github.jlangch.aviron.impl.commands.CommandDef;
import com.github.jlangch.aviron.impl.commands.CommandFormat;


/**
 * Scan a file or a directory (recursively) with archive support enabled 
 * (if not disabled in clamd.conf). A full path is required. 
 * 
 * <p>Stops scanning when a virus is found
 */
public class Scan extends ScanCommand {

    public Scan(final String path) {
        this.path = path;
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("SCAN", CommandFormat.NULL_CHAR);
    }

    @Override
    protected String rawCommand() {
        return String.format(
                  "%s%s %s%s", 
                  getFormat().getPrefix(), 
                  getCommandString(), 
                  path, 
                  getFormat().getTerminator());
    }


    private final String path;
}