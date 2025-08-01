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
package com.github.jlangch.aviron.util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * The FileWatcherQueue is buffering file watching events between the event
 * producing file watcher and asynchronous AV scanner event consumer.
 * 
 * <pre>
 * 
 * +------------+    +-------------+                   +-----------+    +-------+
 * | Filesystem |--->| FileWatcher |--->|  Queue  |--->| AV Client |--->| Clamd |
 * +------------+    +-------------+    +---------+    +-----------+    +-------+
 * 
 * </pre>
 * 
 * <p> The FileWatcherQueue has overflow protection to keep a all operation non 
 * blocking. If the queue grows beyond the max size the oldest entries will be 
 * removed that the entries fit into the queue.
 * 
 * <p>The FileWatcherQueue never blocks and never grows beyond limits to protect
 * the system!
 * 
 * <p>File watchers (like the Java WatchService or the 'fswatch' tool) have the 
 * same behavior. If they get overrun with file change events they discard events 
 * and signal it by sending an 'OVERFLOW' event to their clients.
 */
public class FileWatcherQueue {

    public FileWatcherQueue() {
        this(DEFAULT_SIZE);
    }

    public FileWatcherQueue(final int maxSize) {
        this.maxSize = Math.max(MIN_SIZE, maxSize);
    }

    public int size() {
        synchronized(queue) {
            return queue.size();
        }
    }

    public boolean isEmpty() {
        synchronized(queue) {
            return queue.isEmpty();
        }
    }

    public void clear() {
        synchronized(queue) {
            queue.clear();
        }
    }

    public void remove(final File file) {
        if (file != null) {
            synchronized(queue) {
                queue.removeIf(it -> it.equals(file));
            }
        }
    }

    public void push(final File file) {
        if (file != null) {
            synchronized(queue) {
            	// we're only interested in the last file 'event', so we
            	// can do some optimizations (btw 'fswatch' is doing the
            	// same on a shorter time horizon)
            	//
            	// e.g.:   file create -> push  -> discard
            	//         file modify -> push  -> discard
            	//         file modify -> push  -> discard
            	//         file modify -> push  -> interested in
                queue.removeIf(it -> it.equals(file));

                // overflow: limit the size (discard oldest entries)
                while(queue.size() >= maxSize) {
                    queue.removeFirst();
                }

                queue.add(file);
            }
        }
    }

    public File pop() {
        synchronized(queue) {
            return queue.isEmpty() ? null : queue.removeFirst();
        }
    }

    public File pop(final boolean existingFilesOnly) {
        if (existingFilesOnly) {
            synchronized(queue) {
                while(!queue.isEmpty()) {
                    final File file = queue.removeFirst();
                    if (file.exists()) return file;
                }
                return null;
            }
        }
        else {
            return pop();
        }
    }

    public List<File> pop(final int n) {
        synchronized(queue) {
            final List<File> files = new ArrayList<>(n);
            for(int ii=0; ii<n && !queue.isEmpty(); ii++) {
                files.add(queue.removeFirst());
            }
            return files;
        }
    }

    public List<File> pop(final int n, final boolean existingFilesOnly) {
        if (existingFilesOnly) {
            synchronized(queue) {
                final List<File> files = new ArrayList<>(n);
                while(files.size() < n && !queue.isEmpty()) {
                    final File file = queue.removeFirst();
                    if (file.exists()) files.add(file);
                }
                return files;
            }
        }
        else {
            return pop(n);
        }
    }


    public static final int DEFAULT_SIZE = 1000;
    public static final int MIN_SIZE = 5;

    private final int maxSize;
    private final LinkedList<File> queue = new LinkedList<>();
}
