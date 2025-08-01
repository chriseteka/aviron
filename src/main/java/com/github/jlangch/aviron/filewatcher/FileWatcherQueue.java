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
package com.github.jlangch.aviron.filewatcher;

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
 * +------------+   +-------------+                 +-----------+   +-------+
 * | Filesystem | ⇨ | FileWatcher | ⇨ |  Queue  | ⇨ | AV Client | ⇨ | Clamd |
 * +------------+   +-------------+   +---------+   +-----------+   +-------+
 * 
 * </pre>
 * 
 * <p> The FileWatcherQueue has overflow protection to keep a all operations non 
 * blocking. If the queue grows beyond the max size the oldest entries will be 
 * removed that the new entries fit into the queue.
 * 
 * <p>The FileWatcherQueue never blocks and never grows beyond limits to protect
 * the system!
 * 
 * <p>File watchers (like the Java WatchService or the 'fswatch' tool) have the 
 * same behavior. If they get overrun with file change events they discard events 
 * and signal it by sending an 'OVERFLOW' event to their clients.
 */
public class FileWatcherQueue {

    /**
     * Create a new queue with a capacity of 1000 files
     * 
     * @see #QUEUE_DEFAULT_CAPACITY
     * @see #QUEUE_MIN_CAPACITY
     * @see #QUEUE_MAX_CAPACITY
     */
    public FileWatcherQueue() {
        this(QUEUE_DEFAULT_CAPACITY);
    }

    /**
     * Create a new queue with a capacity
     * 
     * @param capacity the queue's capacity (limited to the range 
     *                 QUEUE_MIN_CAPACITY ... QUEUE_MAX_CAPACITY)
     * 
     * @see #QUEUE_DEFAULT_CAPACITY
     * @see #QUEUE_MIN_CAPACITY
     * @see #QUEUE_MAX_CAPACITY
     */
    public FileWatcherQueue(final int capacity) {
        this.capacity = Math.max(QUEUE_MIN_CAPACITY, 
                                 Math.min(QUEUE_MAX_CAPACITY, capacity));
    }

    /**
     * Returns the queue size
     * 
     * @return the queue size
     */
    public int size() {
        synchronized(queue) {
            return queue.size();
        }
    }

    /**
     * Returns the queue's capacity
     * 
     * @return the queue capacity
     */
    public int capacity() {
        synchronized(queue) {
            return capacity;
        }
    }

    /**
     * Checks if the queue is empty
     * 
     * @return <code>true</code> if the queue is empty else <code>false</code>
     */
    public boolean isEmpty() {
        synchronized(queue) {
            return queue.isEmpty();
        }
    }

    /**
     * Clears the queue
     */
    public void clear() {
        synchronized(queue) {
            queue.clear();
        }
    }

    /**
     * Removes a file form the queue.
     * 
     * @param file the file to be removed
     */
    public void remove(final File file) {
        if (file != null) {
            synchronized(queue) {
                queue.removeIf(it -> it.equals(file));
            }
        }
    }

    /**
     * Push a file to the tail of the queue
     * 
     * <p>Whenever pushing a file to a full queue the queue's head
     * element is discarded in favor of pushing the new file. The
     * overflow count is incremented in this case.
     *
     * @param file the file to push
     * 
     * @see #pop()
     * @see #pop(int)
     * @see #pop(boolean)
     * @see #pop(int, boolean)
     */
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
                if (queue.size() >= capacity) {
                    overflowCount++;
                    while(queue.size() >= capacity) {
                       queue.removeFirst();
                   }
                }

                queue.add(file);
            }
        }
    }

    /**
     * Pops the next file from the head of the queue
     * 
     * @return the next file from the queue or <code>null</code> if the queue
     *         is empty
     * 
     * @see #push(File)
     * @see #pop(int)
     * @see #pop(boolean)
     * @see #pop(int, boolean)
     */
    public File pop() {
        synchronized(queue) {
            return queue.isEmpty() ? null : queue.removeFirst();
        }
    }

    /**
     * Pops the next file from the head of the queue.
     * 
     * <p>Discards any queue head files that are not existing if the parameter 
     * 'existingFilesOnly' is <code>true</code>.
     * 
     * @param existingFilesOnly if <code>true</code> returns only existing files
     * @return the next file from the queue or <code>null</code> if the queue
     *         is empty
     * 
     * @see #push(File)
     * @see #pop()
     * @see #pop(int)
     * @see #pop(int, boolean)
     */
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

    /**
     * Pops the next n files from the head of the queue
     * 
     * @param n the number of files to pop from the queue
     * @return the next n files from the queue. Reads only as many
     *         files from the queue as are available. 
     * 
     * @see #push(File)
     * @see #pop()
     * @see #pop(boolean)
     * @see #pop(int, boolean)
     */
    public List<File> pop(final int n) {
        synchronized(queue) {
            final List<File> files = new ArrayList<>(n);
            for(int ii=0; ii<n && !queue.isEmpty(); ii++) {
                files.add(queue.removeFirst());
            }
            return files;
        }
    }

    /**
     * Pops the next n files from the head of the queue
     * 
     * <p>Discards any queue head files that are not existing if the parameter 
     * 'existingFilesOnly' is <code>true</code>.
     * 
     * @param n the number of files to take from the queue
     * @param existingFilesOnly if <code>true</code> returns only existing files
     * @return the next n files from the queue. Reads only as many
     *         files from the queue as are available. 
     * 
     * @see #push(File)
     * @see #pop()
     * @see #pop(int)
     * @see #pop(boolean)
     */
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


    /**
     * Returns the overflow event count.
     * 
     * <p>Whenever pushing a file to a full queue the queue's head
     * element is discarded in favor of pushing the new file. The
     * overflow count is incremented in this case.
     * 
     * @return the overflow count
     */
    public int overflowCount() {
        synchronized(queue) {
            return overflowCount;
        }
    }

    /**
     * Resets the overflow count
     */
    public void resetOverflowCount() {
        synchronized(queue) {
            overflowCount = 0;
        }
    }


    public static final int QUEUE_DEFAULT_CAPACITY = 1_000;
    public static final int QUEUE_MIN_CAPACITY = 5;
    public static final int QUEUE_MAX_CAPACITY = 100_000;

    private final int capacity;
    private final LinkedList<File> queue = new LinkedList<>();

    private int overflowCount;
}
