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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.event.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.event.FileWatchEvent;
import com.github.jlangch.aviron.filewatcher.event.FileWatchEventType;
import com.github.jlangch.aviron.filewatcher.event.FileWatchRegisterEvent;
import com.github.jlangch.aviron.filewatcher.event.FileWatchTerminationEvent;


public class FileWatcher implements Closeable {

    public FileWatcher(
            final Path dir,
            final Consumer<FileWatchEvent> eventListener,
            final Consumer<FileWatchErrorEvent> errorListener,
            final Consumer<FileWatchTerminationEvent> terminationListener,
            final Consumer<FileWatchRegisterEvent> registerListener
    ) throws IOException {
        this.ws = dir.getFileSystem().newWatchService();
        this.errorListener = errorListener;
        this.registerListener = registerListener;

        register(dir);

        final Runnable runnable = () -> {
            while (true) {
                try {
                    final WatchKey key = ws.take();
                    if (key == null) {
                        break;
                    }

                    final Path dirPath = keys.get(key);
                    if (dirPath == null) {
                        continue;
                    }

                    key.pollEvents()
                       .stream()
                       .filter(e -> (e.kind() != OVERFLOW))
                       .forEach(e -> {
                           @SuppressWarnings("unchecked")
                           final Path p = ((WatchEvent<Path>)e).context();
                           final Path absPath = dirPath.resolve(p);
                           if (absPath.toFile().isDirectory() && e.kind() == ENTRY_CREATE) {
                               // register the new subdir
                               register(ws, keys, absPath, true);
                           }
                           final FileWatchEventType type = convertEvent(e.kind());
                           if (type != null) {
                               safeRun(() -> eventListener.accept(new FileWatchEvent(absPath, type)));
                           }
                         });

                    key.reset();
                }
                catch(ClosedWatchServiceException ex) {
                    break;
                }
                catch(InterruptedException ex) {
                    // continue
                }
                catch(Exception ex) {
                    if (errorListener != null) {
                        safeRun(() -> errorListener.accept(new FileWatchErrorEvent(dir, ex)));
                    }
                    // continue
                }
            }

            try { ws.close(); } catch(Exception e) {}

            if (terminationListener != null) {
                safeRun(() -> terminationListener.accept(new FileWatchTerminationEvent(dir)));
            }
        };

        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("aviron-filewatcher-" + threadCounter.getAndIncrement());
        thread.start();
    }

    public void register(final Path dir) throws IOException {
        if (!dir.toFile().exists()) {
            throw new FileWatcherException("Folder " + dir + " does not exist");
        }
        if (!dir.toFile().isDirectory()) {
            throw new FileWatcherException("Folder " + dir + " is not a directory");
        }

        register(ws, keys, dir, false);
    }

    public List<Path> getRegisteredPaths() {
        return keys.values().stream().sorted().collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        ws.close();
    }

    
    private FileWatchEventType convertEvent(final WatchEvent.Kind<?> kind) {
        if (kind == null) {
            return null;
        }
        else {
            switch(kind.name()) {
                case "ENTRY_CREATE": return FileWatchEventType.CREATED;
                case "ENTRY_DELETE": return FileWatchEventType.DELETED;
                case "ENTRY_MODIFY": return FileWatchEventType.MODIFIED;
                case "OVERFLOW":     return FileWatchEventType.OVERFLOW;
                default:             return null;
            }
        }
    }

    private void register(
            final WatchService ws,
            final Map<WatchKey,Path> keys,
            final Path dir,
            final boolean audit
    ) {
        try {
            final WatchKey dirKey = dir.register(
                    ws,
                    new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY },
                    new WatchEvent.Modifier[0]);

            keys.put(dirKey, dir);

            if (audit && registerListener != null) {
                safeRun(() -> registerListener.accept(new FileWatchRegisterEvent(dir)));
            }
        }
        catch(Exception e) {
            if (errorListener != null) {
                safeRun(() -> errorListener.accept(new FileWatchErrorEvent(dir, e)));
            }
        }
    }

    private void safeRun(final Runnable r) {
        try {
            r.run();
        }
        catch(Exception e) { }
    }


    private static final AtomicLong threadCounter = new AtomicLong(1L);

    private final WatchService ws;
    private final Map<WatchKey,Path> keys = new HashMap<>();
    private final Consumer<FileWatchErrorEvent> errorListener;
    private final Consumer<FileWatchRegisterEvent> registerListener;
}
