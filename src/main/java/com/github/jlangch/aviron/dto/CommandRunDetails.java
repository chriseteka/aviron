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
package com.github.jlangch.aviron.dto;


public class CommandRunDetails {

    public CommandRunDetails(
            final String command, 
            final String response,
            final long elapsedMillis
    ) {
        this.command = command.replace(str('\n'), "[n")
                              .replace(str('\0'), "[0");

        this.response = response;
        this.elapsedMillis = elapsedMillis;
    }


    public String getCommand() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public String getElapsedMillisFormatted() {
        if (elapsedMillis < 1000) {
            return String.format("%dms", elapsedMillis);
        }
        else if (elapsedMillis < 60000){
            return String.format("%ds %dms", elapsedMillis / 1000, elapsedMillis % 1000);
        }
        else {
            final long seconds = elapsedMillis / 1000;
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        }
    }


    @Override
    public String toString() {
        return "CommandRunDetails (" + getElapsedMillisFormatted() + "):\n" +
               command + "\n\n" +
               response;
    }


    private static String str(final char ch) {     
        return String.format("%s", ch);   // handles properly '\0' !!!
    }


    private final String command;
    private final String response;
    private final long elapsedMillis;
}
