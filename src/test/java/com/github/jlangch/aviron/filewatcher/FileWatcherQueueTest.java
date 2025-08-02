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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.util.DemoFilestore;


class FileWatcherQueueTest {

    @Test 
    void testNewQueue() {
        final FileWatcherQueue queue = new FileWatcherQueue();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test 
    void testSize() {
        assertEquals(FileWatcherQueue.QUEUE_DEFAULT_CAPACITY, 
                     new FileWatcherQueue().capacity());

        assertEquals(FileWatcherQueue.QUEUE_MIN_CAPACITY, 
                     new FileWatcherQueue(-1).capacity());

        assertEquals(FileWatcherQueue.QUEUE_MIN_CAPACITY,
                     new FileWatcherQueue(0).capacity());

        assertEquals(FileWatcherQueue.QUEUE_MAX_CAPACITY, 
                     new FileWatcherQueue(Integer.MAX_VALUE).capacity());
    }

    @Test 
    void testPushPop() {
        final FileWatcherQueue queue = new FileWatcherQueue();

        queue.push(new File("1.txt"));
        queue.push(new File("2.txt"));

        assertFalse(queue.isEmpty());
        assertEquals(2, queue.size());

        assertEquals(new File("1.txt"), queue.pop());
        assertEquals(new File("2.txt"), queue.pop());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());

        // non blocking pop
        assertNull(queue.pop());
    }

    @Test 
    void testPushNPop() {
        final FileWatcherQueue queue = new FileWatcherQueue();

        queue.push(new File("1.txt"));
        queue.push(new File("2.txt"));
        queue.push(new File("3.txt"));
        queue.push(new File("4.txt"));
        queue.push(new File("5.txt"));

        assertFalse(queue.isEmpty());
        assertEquals(5, queue.size());

        List<File> f1 = queue.pop(2);
        assertEquals(2, f1.size());

        assertEquals(new File("1.txt"), f1.get(0));
        assertEquals(new File("2.txt"), f1.get(1));

        assertEquals(3, queue.size());

        // non blocking pop
        List<File> f2 = queue.pop(10);
        assertEquals(3, f2.size());

        assertEquals(new File("3.txt"), f2.get(0));
        assertEquals(new File("4.txt"), f2.get(1));
        assertEquals(new File("5.txt"), f2.get(2));

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        
        // non blocking pop
        List<File> f3 = queue.pop(10);
        assertEquals(0, f3.size());
    }

    @Test 
    void testClear() {
        final FileWatcherQueue queue = new FileWatcherQueue();

        queue.push(new File("1.txt"));
        queue.push(new File("2.txt"));
        queue.push(new File("3.txt"));

        assertEquals(3, queue.size());

        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test 
    void testRemove() {
        final FileWatcherQueue queue = new FileWatcherQueue();

        queue.push(new File("1.txt"));
        queue.push(new File("2.txt"));
        queue.push(new File("3.txt"));

        assertEquals(3, queue.size());

        queue.remove(new File("2.txt"));

        assertEquals(2, queue.size());

        assertEquals(new File("1.txt"), queue.pop());
        assertEquals(new File("3.txt"), queue.pop());
        assertNull(queue.pop());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test 
    void testOverflow() {
        final FileWatcherQueue queue = new FileWatcherQueue(5);

        queue.push(new File("1.txt"));
        queue.push(new File("2.txt"));
        queue.push(new File("3.txt"));
        queue.push(new File("4.txt"));
        queue.push(new File("5.txt"));
        queue.push(new File("6.txt"));
        queue.push(new File("7.txt"));

        assertEquals(5, queue.size());

        assertEquals(2, queue.overflowCount());
        queue.resetOverflowCount();
        assertEquals(0, queue.overflowCount());

        assertEquals(new File("3.txt"), queue.pop());
        assertEquals(new File("4.txt"), queue.pop());
        assertEquals(new File("5.txt"), queue.pop());
        assertEquals(new File("6.txt"), queue.pop());
        assertEquals(new File("7.txt"), queue.pop());
        assertNull(queue.pop());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test 
    void testDuplicateDiscardOptimizations() {
        final FileWatcherQueue queue = new FileWatcherQueue(5);

        queue.push(new File("1.txt"));  // discarded
        queue.push(new File("2.txt"));  // discarded
        queue.push(new File("3.txt"));  // discarded
        queue.push(new File("2.txt"));  // discarded
        queue.push(new File("3.txt"));
        queue.push(new File("2.txt"));
        queue.push(new File("1.txt"));

        assertEquals(3, queue.size());

        assertEquals(new File("3.txt"), queue.pop());
        assertEquals(new File("2.txt"), queue.pop());
        assertEquals(new File("1.txt"), queue.pop());
        assertNull(queue.pop());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test 
    void testPopExistingFilesOnly() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");

            final File f1 = demoFS.createFilestoreFile("000", "1.txt");
            final File f2 = demoFS.createFilestoreFile("000", "2.txt");
            final File f3 = demoFS.createFilestoreFile("000", "3.txt");
            
            final FileWatcherQueue queue = new FileWatcherQueue(5);

            queue.push(f1);
            queue.push(f2);
            queue.push(f3);
            
            f2.delete();
            
            assertEquals(f1, queue.pop(true));
            assertEquals(f3, queue.pop(true));
            assertNull(queue.pop());

            assertTrue(queue.isEmpty());
            assertEquals(0, queue.size());
        }
    }

    @Test 
    void testPopNExistingFilesOnly() {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");

            final File f1 = demoFS.createFilestoreFile("000", "1.txt");
            final File f2 = demoFS.createFilestoreFile("000", "2.txt");
            final File f3 = demoFS.createFilestoreFile("000", "3.txt");
            
            final FileWatcherQueue queue = new FileWatcherQueue(5);

            queue.push(f1);
            queue.push(f2);
            queue.push(f3);
            
            f2.delete();
            
            List<File> list = queue.pop(10, true);
            assertEquals(2, list.size());

            assertEquals(f1, list.get(0));
            assertEquals(f3, list.get(1));
            assertNull(queue.pop());

            assertTrue(queue.isEmpty());
            assertEquals(0, queue.size());
        }
    }
}
