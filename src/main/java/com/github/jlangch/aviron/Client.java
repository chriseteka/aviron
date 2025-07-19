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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.commands.mgmt.Ping;
import com.github.jlangch.aviron.commands.mgmt.Reload;
import com.github.jlangch.aviron.commands.mgmt.Shutdown;
import com.github.jlangch.aviron.commands.mgmt.Stats;
import com.github.jlangch.aviron.commands.mgmt.Version;
import com.github.jlangch.aviron.commands.mgmt.VersionCommands;
import com.github.jlangch.aviron.commands.scan.ContScan;
import com.github.jlangch.aviron.commands.scan.InStream;
import com.github.jlangch.aviron.commands.scan.MultiScan;
import com.github.jlangch.aviron.commands.scan.Scan;
import com.github.jlangch.aviron.commands.scan.ScanResult;
import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.ex.UnknownCommandException;
import com.github.jlangch.aviron.server.CommandRunDetails;
import com.github.jlangch.aviron.server.ServerIO;
import com.github.jlangch.aviron.util.Lazy;
import com.github.jlangch.aviron.util.Quarantine;


/**
 * The ClamAV client provides access to the ClamAV daemon (clamd) functions 
 * like file scanning, updating the daemon's ClamAV virus databases, or getting 
 * the scanning stats.
 * 
 * <p>The ClamAV client communicates through a <i>Socket</i> with the
 * <i>clamd</i> daemon.
 * 
 * <pre>
 * Client client = Client.builder()
 *                       .serverHostname("localhost")
 *                       .serverFileSeparator(FileSeparator.UNIX)
 *                       .build();
 *
 * System.out.println(client.version());
 * 
 * client.reloadVirusDatabases();
 *
 * ScanResult result = client.scan(Path.get("/data/summary.docx"));
 * if (result.hasVirus()) {
 *    System.out.println(result.getVirusFound());
 * }
 * </pre>
 * 
 * For testing purposes start clamd in the foreground:
 * <pre>
 * // foreground
 * clamd --foreground
 * clamd --log=/tmp/clamd.log --pid=/tmp/clamd.pid --foreground
 * 
 * // background
 * clamd --log=/tmp/clamd.log --pid=/tmp/clamd.pid
 * </pre>
 * 
 * @see <a href="https://docs.clamav.net/manual/Usage.html">ClamAV Manual</a>
 * @see <a href="https://linux.die.net/man/8/clamd">Clamd Man Pages</a>
 * @see <a href="https://www.liquidweb.com/blog/install-clamav/">Install ClamAV</a>
 * @see <a href="https://truehost.com/support/knowledge-base/how-to-install-clamav-for-malware-scanning-on-linux/">Install ClamAV on AlmaLinux</a>
 */
public class Client {

    private Client(final Builder builder) {
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
                        "The quarantine directory «" + builder.quarantineDir + "» has not write permission!");
            }
        }

        this.server = new ServerIO(
                            builder.serverHostname,
                            builder.serverPort,
                            builder.serverFileSeparator,
                            builder.connectionTimeoutMillis,
                            builder.readTimeoutMillis);
        
        this.quarantine = new Quarantine(
                                builder.quarantineFileAction,
                                builder.quarantineDir,
                                builder.quarantineActionListener);
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
        return sendCommand(new Ping());
    }

    /**
     * Return the ClamAV version
     * 
     * @return the ClamAV version
     */
    public String version() {
        return sendCommand(new Version());
    }

    /**
     * Returns the statistics about the scan queue, contents of scan queue, and 
     * memory usage.
     *
     * @return the formatted scanning statistics
     */
    public String stats() {
        return sendCommand(new Stats());
    }

    /**
     * Reload the virus databases. 
     */
    public void reloadVirusDatabases() {
        sendCommand(new Reload());
    }

    /**
     * Shutdown the ClamAV server and perform a clean exit.
     */
    public void shutdownServer() {
        sendCommand(new Shutdown());
    }

    /**
     * Scans a file's data passed in the stream. Uses the default chunk size of
     * 2048 bytes.
     * 
     * Note: The input stream must be closed by the caller!
     * 
     * @param inputStream the file data to scan
     * @return the scan result
     */
    public ScanResult scan(final InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("An 'inputStream' must not be null!");
        }

        final ScanResult result = scan(inputStream, InStream.DEFAULT_CHUNK_SIZE);
        quarantine.handleQuarantineActions(result);
        return result;
    }

    /**
     * Scans a file's data passed in the stream.
     * 
     * Note: The input stream must be closed by the caller!
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

        final ScanResult result = sendCommand(new InStream(inputStream, chunkSize));
        quarantine.handleQuarantineActions(result);
        return result;
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

        final ScanResult result = scan(path, false);
        quarantine.handleQuarantineActions(result);
        return result;
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

        final String serverPath = server.toServerPath(path);
        final ScanResult result = continueScan 
                                    ? sendCommand(new ContScan(serverPath))
                                    : sendCommand(new Scan(serverPath));
        quarantine.handleQuarantineActions(result);
        return result;
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

        final ScanResult result = sendCommand(new MultiScan(server.toServerPath(path)));
        quarantine.handleQuarantineActions(result);
        return result;
    }

    /**
     * Tests if the ClamAV server is reachable. Uses the default timeout
     * of 3'000ms.
     * 
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable() {
        return server.isReachable();
    }

    /**
     * Tests if the ClamAV server is reachable.
     * 
     * @param timeoutMillis  the timeout in milliseconds
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable(final int timeoutMillis) {
        return server.isReachable(timeoutMillis);
    }

    /**
     * Returns the raw command string and the server's result for
     * the last command sent to the ClamAV server.
     * 
     * This function is provided for debugging
     * 
     * @return the details on the last command run
     */
    public CommandRunDetails getLastCommandRunDetails() {
        return server.getLastCommandRunDetails();
    }


    private List<String> loadAvailableCommands() {
        return new VersionCommands().send(server);
    }

    private <T> T sendCommand(final Command<T> command) {
        try {
            if (memoizedAvCommands.get().contains(command.getCommandString())) {
                return command.send(server);
            }

            throw new UnknownCommandException(command.getCommandString());
        }
        catch (AvironException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new AvironException(
                    String.format("Failed to send command: %s", command.getCommandString()),
                    ex);
        }
    }


    public static class Builder {
        public Client build() {
            return new Client(this);
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
         * @param quarantineFileAction a quarantine file action
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
         * A quarantine action listener, that receives all quarantine file action
         * events. Defaults to <code>null</code>.
         *
         * @param listener a quarantine file action listener
         * @return this builder
         */
        public Builder quarantineActionListener(final Consumer<QuarantineActionInfo> listener) {
            this.quarantineActionListener = listener;
            return this;
        }


        private String serverHostname = ServerIO.LOCALHOST;
        private int serverPort = ServerIO.DEFAULT_SERVER_PORT;
        private FileSeparator serverFileSeparator = FileSeparator.JVM_PLATFORM;
        private int connectionTimeoutMillis = ServerIO.DEFAULT_CONNECTION_TIMEOUT;
        private int readTimeoutMillis = ServerIO.DEFAULT_READ_TIMEOUT;
        private QuarantineFileAction quarantineFileAction = QuarantineFileAction.NONE;
        private File quarantineDir = null;
        private Consumer<QuarantineActionInfo> quarantineActionListener;
    }


    public static final String LOCALHOST = "localhost";
    public static final int DEFAULT_SERVER_PORT = ServerIO.DEFAULT_SERVER_PORT;
    public static final FileSeparator DEFAULT_SERVER_PLATFORM = ServerIO.DEFAULT_SERVER_FILESEPARATOR;

    private final Quarantine quarantine;
    private final ServerIO server;
    private final Lazy<List<String>> memoizedAvCommands = new Lazy<>(this::loadAvailableCommands);
}