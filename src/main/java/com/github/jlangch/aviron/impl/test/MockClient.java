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
package com.github.jlangch.aviron.impl.test;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.dto.CommandRunDetails;
import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.impl.quarantine.Quarantine;
import com.github.jlangch.aviron.impl.server.ClamdServerIO;
import com.github.jlangch.aviron.impl.util.AvironVersion;
import com.github.jlangch.aviron.impl.util.CollectionUtils;


/**
 * Mock client
 */
public class MockClient {

    private MockClient(final Builder builder) {
        if (builder.serverHostname == null || builder.serverHostname.isEmpty()) {
            throw new IllegalArgumentException("The server hostname must not be null or empty!");
        }
        if (builder.serverPort <= 0) {
            throw new IllegalArgumentException("The server port must not be negative!");
        }
        if (builder.serverFileSeparator == null) {
            throw new IllegalArgumentException("The server file separator must not be null!");
        }
        if (builder.connectionTimeoutMillis < 0) {
            throw new IllegalArgumentException("The connection timeout must not be negative!");
        }
        if (builder.readTimeoutMillis < 0) {
            throw new IllegalArgumentException("The read timeout must not be negative!");
        }
        if (builder.quarantineDir != null) {
            if (!builder.quarantineDir.isDirectory()) {
                throw new IllegalArgumentException(
                        "The quarantine directory «" + builder.quarantineDir + "» does not exist!");
            }
            if (!builder.quarantineDir.canWrite()) {
                throw new IllegalArgumentException(
                        "The quarantine directory «" + builder.quarantineDir + "» has no write permission!");
            }
        }
        if (builder.quarantineFileAction != QuarantineFileAction.NONE) {
            if (builder.quarantineDir == null) {
                throw new IllegalArgumentException(
                        "A quarantine directory is required if the QuarantineFileAction is not NONE!");
            }
        }

        this.server = new ClamdServerIO(
                            builder.serverHostname,
                            builder.serverPort,
                            builder.serverFileSeparator,
                            builder.connectionTimeoutMillis,
                            builder.readTimeoutMillis);
        
        this.quarantine = new Quarantine(
                                builder.quarantineFileAction,
                                builder.quarantineDir,
                                builder.quarantineEventListener);
    }


    /**
     * Return a client builder
     * 
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Sends a "PING" command to the ClamAV server.
     * 
     * @return <code>true</code> if the server answers with a "PONG" else 
     *         <code>false</code>.
     */
    public boolean ping() {
        return true;
    }

    /**
     * Return the ClamAV version
     * 
     * @return the ClamAV version
     */
    public String clamAvVersion() {
        return "1.0.0";
    }

    /**
     * Returns the statistics about the scan queue, contents of scan queue, and 
     * memory usage.
     *
     * @return the formatted scanning statistics
     */
    public String stats() {
        return "xxxxx";
    }

    /**
     * Reload the virus databases. 
     */
    public void reloadVirusDatabases() {
    }

    /**
     * Shutdown the ClamAV server and perform a clean exit.
     */
    public void shutdownServer() {
    }

    /**
     * Scans a file's data passed in the stream. Uses the default chunk size of
     * 2048 bytes.
     * 
     * <p>Note 1: The input stream must be closed by the caller!
     * 
     * <p>Note 2: There is no quarantine action for streamed data
     * 
     * @param inputStream the file data to scan
     * @return the scan result
     */
    public ScanResult scan(final InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("An 'inputStream' must not be null!");
        }

        try {
            byte[] buf=new byte[4096];
            while ((inputStream.read(buf))!=-1) { }
        }
        catch(Exception ex) { }

        return ScanResult.ok();
    }

    /**
     * Scans a file's data passed in the stream.
     * 
     * <p>Note 1: The input stream must be closed by the caller!
     * 
     * <p>Note 2: There is no quarantine action for streamed data
     * 
     * @param inputStream the file data to scan
     * @param chunkSize the chunk size to use when reading data chunks from 
     *                  the stream
     * @return the scan result
     */
    public ScanResult scan(final InputStream inputStream, final int chunkSize) {
        if (inputStream == null) {
            throw new IllegalArgumentException("An 'inputStream' must not be null!");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("A 'chunkSize' must be greater than 0");
        }

        try {
            byte[] buf=new byte[4096];
            while ((inputStream.read(buf))!=-1) { }
        }
        catch(Exception ex) { }

        return ScanResult.ok();
    }

    /**
     * Scans a single file or directory (recursively). Stops after the first file 
     * with a virus.
     * 
     * @param path  a file or directory
     * @return the scan result
     */
    public ScanResult scan(final Path path) {
        if (path == null) {
            throw new IllegalArgumentException("A 'path' must not be null!");
        }

        if (Files.isRegularFile(path)
            && path.toFile().getName().toLowerCase().contains("eicar")
        ) {
            final Map<String, List<String>> viruses = new HashMap<>();
            viruses.put(path.toFile().getName(), CollectionUtils.toList("Eicar virus signature"));
            final ScanResult result = ScanResult.virusFound(viruses);
            quarantine.handleQuarantineActions(result);
            return result;
       }
        else {
            final ScanResult result = ScanResult.ok();
            quarantine.handleQuarantineActions(result);
           return result;
        }
    }

    /**
     * Scans a single file or directory (recursively).
     * 
     * @param path  a file or directory
     * @param continueScan  if <code>true</code> continues scanning upon detecting 
     *                      a virus in a file else stops after the first file with 
     *                      a virus.
     * @return the scan result
     */
    public ScanResult scan(final Path path, final boolean continueScan) {
        if (path == null) {
            throw new IllegalArgumentException("A 'path' must not be null!");
        }

        return scan(path);
    }

    /**
     * Scans a single file or directory (recursively) using multiple threads.
     * 
     * @param path  a file or directory
     * @return the scan result
     */
    public ScanResult parallelScan(final Path path) {
        if (path == null) {
            throw new IllegalArgumentException("A 'path' must not be null!");
        }

        return scan(path);
    }

    /**
     * Tests if the ClamAV server is reachable. Uses the default timeout
     * of 3'000ms.
     * 
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable() {
        return true;
    }

    /**
     * Tests if the ClamAV server is reachable.
     * 
     * @param timeoutMillis  the timeout in milliseconds
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable(final int timeoutMillis) {
        return true;
    }

    /**
     * Returns the raw command string and the server's result for
     * the last command sent to the ClamAV server.
     * 
     * This function is provided for debugging
     * 
     * @return the details on the last command run
     */
    public CommandRunDetails lastCommandRunDetails() {
        return null;
    }

    /**
     * Checks whether the quarantine is active
     * 
     * @return <code>true</code> if quarantine is active else <code>false</code>
     */
    public boolean isQuarantineActive() {
        return quarantine.isActive();
    }

    /**
     * Returns a list of the quarantine files.
     * 
     * @return a list of the quarantined files
     */
    public List<QuarantineFile> listQuarantineFiles() {
        return quarantine.isActive() 
                ? quarantine.listQuarantineFiles()
                : new ArrayList<>();
    }

    /**
     * Removes a quarantine file.
     * 
     * <p>Silently ignores the request if the quarantine file does not exist.
     * 
     * @param file  the quarantine file to remove
     */
    public void removeQuarantineFile(final QuarantineFile file) {
        if (file == null) {
            throw new IllegalArgumentException("A 'file' must not be null!");
        }

        quarantine.removeQuarantineFile(file); 
    }

    /**
     * Removes all quarantine file.s
     */
    public void removeAllQuarantineFiles() {
        quarantine.removeAllQuarantineFiles(); 
    }

    /**
     * Print the quarantine file info in human readable form to a <code>PrintStream</code>
     * 
     * @param stream  the print stream. If <code>null</code> prints to stdout.
     */
    public void printQuarantineInfo(final PrintStream stream) {
        final PrintStream ps = stream == null ? System.out : stream;
        
        quarantine.listQuarantineFiles().forEach(f -> {
            ps.println(f.getQuarantineFileName());
            ps.println("    " + f.getInfectedFile());
            ps.println("    " + f.getVirusListFormatted());
            ps.println("    " + f.getAction());
            ps.println("    " + f.getQuarantinedAt());
            ps.println();
        });
    }

    /**
     * Print the client configuration in human readable form to a <code>PrintStream</code>
     * 
     * @param stream  the print stream. If <code>null</code> prints to stdout.
     */
    public void printConfig(final PrintStream stream) {
        final PrintStream ps = stream == null ? System.out : stream;
        ps.println(toString());
    }

    /**
     * Returns the version of this Aviron library
     * 
     * @return the version
     */
    public String version() {
        return AvironVersion.VERSION;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("serverHostname: ");
        sb.append(server.getHostname());
        sb.append(System.lineSeparator());

        sb.append("serverPort: ");
        sb.append(server.getPort());
        sb.append(System.lineSeparator());

        sb.append("serverFileSeparator: ");
        sb.append(server.getFileSeparator());
        sb.append(System.lineSeparator());

        sb.append("connectionTimeoutMillis: ");
        sb.append(server.getConnectionTimeoutMillis());
        sb.append(System.lineSeparator());

        sb.append("readTimeoutMillis: ");
        sb.append(server.getReadTimeoutMillis());
        sb.append(System.lineSeparator());

        sb.append("quarantineFileAction: ");
        sb.append(quarantine.getQuarantineFileAction());
        sb.append(System.lineSeparator());

        sb.append("quarantineDir: ");
        sb.append(formatConfig(quarantine.getQuarantineDir()));
        sb.append(System.lineSeparator());

        sb.append("quarantineListener: ");
        sb.append(formatConfig(quarantine.hasListener()));

        return sb.toString();
     }


    private String formatConfig(final File f) {
        return f != null ? f.getPath() : "-";
    }

    private String formatConfig(final boolean b) {
        return b ? "supplied" : "-";
    }


    public static class Builder {
        public MockClient build() {
            return new MockClient(this);
        }

        /**
         *  The ClamAV server hostname. Defaults to <code>localhost</code>
         *  
         * @param hostname server hostname
         * @return this builder
         */
        public Builder serverHostname(final String hostname) {
            this.serverHostname = hostname;
            return this;
        }

        /** 
         * The ClamAV server port. Defaults to <code>3310</code> 
         *  
         * @param port server port
         * @return this builder
         */
        public Builder serverPort(final int port) {
            this.serverPort = port;
            return this;
        }

        /** 
         * The ClamAV server file separator. Defaults to <code>FileSeparator.JVM_PLATFORM</code> 
         *  
         * @param separator server file separator
         * @return this builder
         */
        public Builder serverFileSeparator(final FileSeparator separator) {
            this.serverFileSeparator = separator;
            return this;
        }

        /** 
         * The connection timeout, 0 means indefinite. Defaults to <code>3'000ms</code> 
         *  
         * @param timeoutMillis connection timeout in millis
         * @return this builder
         */
        public Builder connectionTimeout(final int timeoutMillis) {
            this.connectionTimeoutMillis = timeoutMillis;
            return this;
        }

        /** 
         * The read timeout, 0 means indefinite. Defaults to <code>20'000ms</code> 
         *  
         * @param timeoutMillis read timeout in millis
         * @return this builder
         */
        public Builder readTimeout(final int timeoutMillis) {
            this.readTimeoutMillis = timeoutMillis;
            return this;
        }

        /** 
         * A quarantine file action for infected files. Defaults to 
         * <code>QuarantineFileAction.NONE</code> 
         *  
         * @param action a quarantine file action
         * @return this builder
         */
        public Builder quarantineFileAction(final QuarantineFileAction action) {
            this.quarantineFileAction = action == null ? QuarantineFileAction.NONE : action;
            return this;
        }

        /** 
         * A quarantine directory where the infected files are move/copied to 
         * depending on the configured quarantine file action. Defaults to 
         * <code>null</code>.
         *  
         * @param quarantineDir a quarantine directory
         * @return this builder
         */
        public Builder quarantineDir(final File quarantineDir) {
            this.quarantineDir = quarantineDir;
            return this;
        }

        /** 
         * A quarantine directory where the infected files are move/copied to 
         * depending on the configured quarantine file action. Defaults to 
         * <code>null</code>.
         *  
         * @param quarantineDir a quarantine directory
         * @return this builder
         */
        public Builder quarantineDir(final String quarantineDir) {
            this.quarantineDir = quarantineDir == null ? null : new File(quarantineDir);
            return this;
        }

        /** 
         * A quarantine event listener, that receives all quarantine file action
         * events. Defaults to <code>null</code>.
         *
         * @param listener a quarantine event listener
         * @return this builder
         */
        public Builder quarantineEventListener(final Consumer<QuarantineEvent> listener) {
            this.quarantineEventListener = listener;
            return this;
        }


        private String serverHostname = ClamdServerIO.LOCALHOST;
        private int serverPort = ClamdServerIO.DEFAULT_SERVER_PORT;
        private FileSeparator serverFileSeparator = FileSeparator.JVM_PLATFORM;
        private int connectionTimeoutMillis = ClamdServerIO.DEFAULT_CONNECTION_TIMEOUT;
        private int readTimeoutMillis = ClamdServerIO.DEFAULT_READ_TIMEOUT;
        private QuarantineFileAction quarantineFileAction = QuarantineFileAction.NONE;
        private File quarantineDir = null;
        private Consumer<QuarantineEvent> quarantineEventListener;
    }


    private final Quarantine quarantine;
    private final ClamdServerIO server;
}