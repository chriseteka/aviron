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

import static com.github.jlangch.aviron.events.ClamdAwaitingEvent.Status.NotReachable;
import static com.github.jlangch.aviron.events.ClamdAwaitingEvent.Status.Operational;
import static com.github.jlangch.aviron.events.ClamdAwaitingEvent.Status.Reachable;
import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;
import static com.github.jlangch.aviron.util.Util.sleep;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.dto.CommandRunDetails;
import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.events.ClamdAwaitingEvent;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.ex.AvironException;
import com.github.jlangch.aviron.ex.UnknownCommandException;
import com.github.jlangch.aviron.impl.commands.Command;
import com.github.jlangch.aviron.impl.commands.mgmt.Ping;
import com.github.jlangch.aviron.impl.commands.mgmt.Reload;
import com.github.jlangch.aviron.impl.commands.mgmt.Shutdown;
import com.github.jlangch.aviron.impl.commands.mgmt.Stats;
import com.github.jlangch.aviron.impl.commands.mgmt.Version;
import com.github.jlangch.aviron.impl.commands.mgmt.VersionCommands;
import com.github.jlangch.aviron.impl.commands.scan.ContScan;
import com.github.jlangch.aviron.impl.commands.scan.InStream;
import com.github.jlangch.aviron.impl.commands.scan.MultiScan;
import com.github.jlangch.aviron.impl.commands.scan.Scan;
import com.github.jlangch.aviron.impl.quarantine.Quarantine;
import com.github.jlangch.aviron.impl.server.ClamdServerIO;
import com.github.jlangch.aviron.impl.util.AvironVersion;
import com.github.jlangch.aviron.impl.util.CollectionUtils;
import com.github.jlangch.aviron.impl.util.Lazy;


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

        this.mocking = builder.mocking;
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
        return mocking
                ? true
                : sendCommand(new Ping());
    }

    /**
     * Return the ClamAV version
     *
     * @return the ClamAV version
     */
    public String clamAvVersion() {
        return mocking
                ? "0.0.0"
                : sendCommand(new Version());
    }

    /**
     * Returns the statistics about the scan queue, contents of scan queue, and
     * memory usage.
     *
     * @return the formatted scanning statistics
     */
    public String stats() {
        return mocking
                ? ""
                : sendCommand(new Stats());
    }

    /**
     * Reload the virus databases.
     */
    public void reloadVirusDatabases() {
        if (!mocking) {
            sendCommand(new Reload());
        }
    }

    /**
     * Shutdown the ClamAV server and perform a clean exit.
     */
    public void shutdownServer() {
        if (!mocking) {
            sendCommand(new Shutdown());
        }
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

        // there is no quarantine action for streamed data
        return mocking
                ? discard(inputStream)
                : scan(inputStream, InStream.DEFAULT_CHUNK_SIZE);
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

        // there is no quarantine action for streamed data
        return mocking
                ? discard(inputStream)
                : sendCommand(new InStream(inputStream, chunkSize));
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

        if (mocking) {
            return mockScan(path,false);
        }
        else {
            final ScanResult result = scan(path, false);
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

        if (mocking) {
            return mockScan(path, continueScan);
        }
        else {
            final String serverPath = server.toServerPath(path);
            final ScanResult result = continueScan
                                        ? sendCommand(new ContScan(serverPath))
                                        : sendCommand(new Scan(serverPath));
            quarantine.handleQuarantineActions(result);
            return result;
        }
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

        if (mocking) {
            return mockScan(path, true);
        }
        else {
            final ScanResult result = sendCommand(new MultiScan(server.toServerPath(path)));
            quarantine.handleQuarantineActions(result);
            return result;
        }
    }

    /**
     * Tests if the ClamAV server is reachable. Uses the default timeout
     * of 3'000ms.
     *
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable() {
        return mocking
                ? true
                : server.isReachable();
    }

    /**
     * Tests if the ClamAV server is reachable.
     *
     * @param timeoutMillis  the timeout in milliseconds
     * @return <code>true</code> if the server is reachable else <code>false</code>.
     */
    public boolean isReachable(final int timeoutMillis) {
        return mocking
                ? true
                : server.isReachable(timeoutMillis);
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
        return mocking
                ? null
                : server.getLastCommandRunDetails();
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
     * Wait for Clamd to get operational.
     *
     * <p>Checks first if the clamd daemon process is running followed by ping
     * command to verify that clamd is operational and ready to accept commands.
     *
     * <p>Clamd has pretty long startup time due the the expensive loading
     * of the virus database.
     *
     * @param maxWaitTime the max wait time
     * @param unit the max wait time unit
     * @param listener an optional event listener
     * @return <code>true</code> if clamd is operational or <code>false</code> if
     *         clamd did not get operational within the given time frame.
     */
    public boolean waitForOperationalClamd(
            final long maxWaitTime,
            final TimeUnit unit,
            final Consumer<ClamdAwaitingEvent> listener
    ) {
        if (maxWaitTime < 0) {
            throw new IllegalArgumentException("A 'maxWaitTime' must not be negative!");
        }
        if (unit == null) {
            throw new IllegalArgumentException("A 'unit' must not be null!");
        }

        final long maxTime = System.currentTimeMillis() + unit.toMillis(maxWaitTime);

        final Supplier<Boolean> reachable = () -> {
            try { return isReachable(1_000); }
            catch(Exception ex) { return false; }
        };

        final Supplier<Boolean> operational = () -> {
            try { return ping(); }
            catch(Exception ex) { return false; }
        };

        while (System.currentTimeMillis() < maxTime) {
            if (reachable.get()) {
                if (operational.get()) {
                    fireEvent(new ClamdAwaitingEvent(Operational), listener);
                    return true;  // clamd is operational
                }
                else {
                       fireEvent(new ClamdAwaitingEvent(Reachable), listener);
                }
            }
            else {
                fireEvent(new ClamdAwaitingEvent(NotReachable), listener);
            }

            try {
                Thread.sleep(3_000);
            }
            catch(InterruptedException e) {
                return false;
            }
        }

        return false;
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

        if (mocking) {
            sb.append("mocking: ENABLED");
            sb.append(System.lineSeparator());
        }

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


    private ScanResult mockScan(final Path path, final boolean continueScan) {
        sleep(80);  // sleep 80ms to simulate a clamd scan

        if (Files.isRegularFile(path) && isEicarTestFile(path)) {
            final Map<String, List<String>> viruses = new HashMap<>();
            viruses.put(path.toFile().getPath(), CollectionUtils.toList("EICAR-AV-Test"));
            final ScanResult result = ScanResult.virusFound(viruses);
            quarantine.handleQuarantineActions(result);
            return result;
        }
        else if (Files.isDirectory(path)) {
            final List<Path> infected = Arrays.stream(path.toFile().listFiles())
                                              .map(f -> f.toPath())
                                              .filter(p -> Files.isRegularFile(p))
                                              .filter(p -> isEicarTestFile(p))
                                              .collect(Collectors.toList());
            if (infected.isEmpty()) {
                return ScanResult.ok();
            }
            else {
                final List<Path> items = continueScan
                                            ? infected
                                            : infected.subList(0, 1);

                final ScanResult result = ScanResult.virusFound(
                                            items.stream()
                                                 .collect(Collectors.toMap(
                                                             p -> p.toString(),
                                                             p -> toList("EICAR-AV-Test"))));
                quarantine.handleQuarantineActions(result);
                return result;
            }
        }
        else {
            return ScanResult.ok();
        }
    }

    private String formatConfig(final File f) {
        return f != null ? f.getPath() : "-";
    }

    private String formatConfig(final boolean b) {
        return b ? "supplied" : "-";
    }

    private List<String> loadAvailableCommands() {
        return new VersionCommands().send(server);
    }

    private ScanResult discard(final InputStream is) {
        try {
            byte[] buf=new byte[4096];
            while ((is.read(buf))!=-1) { }
        }
        catch(Exception ex) { };

        sleep(80);  // sleep 80ms to simulate a clamd scan

        return ScanResult.ok();
    }

    private boolean isEicarTestFile(final Path path) {
        return Files.isRegularFile(path)
                && path.toFile().getName().toLowerCase().contains("eicar");
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

    private void fireEvent(
            final ClamdAwaitingEvent event,
            final Consumer<ClamdAwaitingEvent> listener
    ) {
        if (listener != null) {
            try {
                listener.accept(event);
            }
            catch(Exception e) { }
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

        /**
         * If set to <code>true</code> enables the mock mode.
         *
         * <p>In mocking mode the client will not physically communicate with
         * the clamd daemon.
         *
         * @param mocking enable/disable mocking. Defaults to <code>false</code>.
         *
         * @return this builder
         */
        public Builder mocking(final boolean mocking) {
            this.mocking = mocking;
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
        private boolean mocking = false;
    }


    public static final String LOCALHOST = "localhost";
    public static final int DEFAULT_SERVER_PORT = ClamdServerIO.DEFAULT_SERVER_PORT;
    public static final FileSeparator DEFAULT_SERVER_PLATFORM = ClamdServerIO.DEFAULT_SERVER_FILESEPARATOR;

    private final Quarantine quarantine;
    private final ClamdServerIO server;
    private final boolean mocking;
    private final Lazy<List<String>> memoizedAvCommands = new Lazy<>(this::loadAvailableCommands);
}