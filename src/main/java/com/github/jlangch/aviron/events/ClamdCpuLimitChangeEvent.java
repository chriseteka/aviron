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
package com.github.jlangch.aviron.events;


public class ClamdCpuLimitChangeEvent implements Event {

    public ClamdCpuLimitChangeEvent(
            final int oldLimit,
            final int newLimit
    ) {
        this.oldLimit = oldLimit;
        this.newLimit = newLimit;
    }



    public int getOldLimit() {
        return oldLimit;
    }

   public int getNewLimit() {
        return newLimit;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Old Limit: ");
        sb.append(oldLimit);
        sb.append(System.lineSeparator());
        sb.append("New Limit: ");
        sb.append(newLimit);

        return sb.toString();
    }


    private final int oldLimit;
    private final int newLimit;
}
