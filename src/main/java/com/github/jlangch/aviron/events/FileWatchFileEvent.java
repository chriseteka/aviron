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


public class FileWatchFileEvent implements Event {

    public FileWatchFileEvent(
            final Path path,
            final boolean isDir,
            final boolean isFile,
            final FileWatchFileEventType type
    ) {
        this.path = path;
        this.isDir = isDir;
        this.isFile = isFile;
        this.type = type;
    }


    public Path getPath() {
        return path;
    }

    public boolean isDir() {
        return isDir;
    }

    public boolean isFile() {
        return isFile;
    }

    public FileWatchFileEventType getType() {
        return type;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Path: ");
        sb.append(path);
        sb.append(System.lineSeparator());
        sb.append("File Type: " + (isDir ? "dir" : (isFile ? "file" : "unknown")));
        sb.append(System.lineSeparator());
        sb.append("Event Type: ");
        sb.append(type);

        return sb.toString();
    }


    private final Path path;
    private final boolean isDir;
    private final boolean isFile;
    private final FileWatchFileEventType type;
}
