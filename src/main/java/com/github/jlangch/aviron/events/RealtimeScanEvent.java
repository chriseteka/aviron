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

import java.nio.file.Path;

import com.github.jlangch.aviron.commands.scan.ScanResult;


public class RealtimeScanEvent implements Event {
    
    public RealtimeScanEvent(final Path path, final ScanResult result) {
        this.path = path;
        this.result = result;
    }


    public Path getPath() {
        return path;
    }

    public ScanResult getScanResult() {
        return result;
    }


    private final Path path;
    private final ScanResult result;
}
