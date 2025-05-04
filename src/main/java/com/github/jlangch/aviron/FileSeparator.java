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
package com.github.jlangch.aviron;

import java.io.File;
import java.nio.file.Path;


public enum FileSeparator {

    UNIX('/') {
        @Override
        public String toServerPath(final Path path) {
            return path.toString().replace(WINDOWS.separator, UNIX.separator);
        }
    },

    WINDOWS('\\') {
        @Override
        public String toServerPath(final Path path) {
            return path.toString().replace(UNIX.separator, WINDOWS.separator);
        }
    },

    JVM_PLATFORM(File.separatorChar) {
        @Override
        public String toServerPath(final Path path) {
            return path.toString();
        }
    };

    public abstract String toServerPath(final Path path);


    private FileSeparator(final char separator) {
        this.separator = separator;
    }

    
    private final char separator;
}