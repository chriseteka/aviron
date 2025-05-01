package com.github.jlangch.aviron;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

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
import com.github.jlangch.aviron.util.OS;
import com.github.jlangch.aviron.util.Shell;
import com.github.jlangch.aviron.util.ShellResult;
import com.github.jlangch.aviron.util.StringUtils;


/**
 * The ClamAV client provides access to the ClamAV functions like file scanning,
 * updating the ClamAV virus databases, or getting the scanning stats.
 * 
 * <pre>
 * Client client = new Client.Builder()
 *                           .serverHostname("localhost")
 *                           .serverFileSeparator(FileSeparator.UNIX)
 *                           .build();
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

        this.server = new ServerIO(
                            builder.serverHostname,
                            builder.serverPort,
                            builder.serverFileSeparator,
                            builder.connectionTimeoutMillis,
                            builder.readTimeoutMillis);
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

        return scan(inputStream, InStream.DEFAULT_CHUNK_SIZE);
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

        return sendCommand(new InStream(inputStream, chunkSize));
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

        return scan(path, false);
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
        return continueScan ? sendCommand(new ContScan(serverPath))
                            : sendCommand(new Scan(serverPath));
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

        return sendCommand(new MultiScan(server.toServerPath(path)));
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


    /**
     * Returns the clamd PID or null if clamd is not running.
     * 
     * <p>
     * Note: This function is available for Linux and MacOS only!
     * 
     * @return the clamd PID
     */
    public String getClamdPID() {
        if (OS.isLinux() || OS.isMacOSX()) {
            try {
                final ShellResult r = Shell.execCmd("pgrep", "clamd");
                if (r.getExitCode() == 0) {
                    return StringUtils
                                .splitIntoLines(r.getStdout())
                                .stream()
                                .filter(s -> !StringUtils.isBlank(s))
                                .findFirst()
                                .orElse(null);
                }
                else {
                    return null;
                }
            }
            catch(IOException ex) {
                throw new AvironException("Failed to get clamd PID", ex);
            }
        }
        else {
            throw new AvironException(
                    "Client::getClamdPid is available for Linux and MacOS only!");
        }
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

        private String serverHostname = ServerIO.LOCALHOST;
        private int serverPort = ServerIO.DEFAULT_SERVER_PORT;
        private FileSeparator serverFileSeparator = FileSeparator.JVM_PLATFORM;
        private int connectionTimeoutMillis = ServerIO.DEFAULT_CONNECTION_TIMEOUT;
        private int readTimeoutMillis = ServerIO.DEFAULT_READ_TIMEOUT;
    }


    public static final String LOCALHOST = "localhost";
    public static final int DEFAULT_SERVER_PORT = ServerIO.DEFAULT_SERVER_PORT;
    public static final FileSeparator DEFAULT_SERVER_PLATFORM = ServerIO.DEFAULT_SERVER_FILESEPARATOR;

    private final ServerIO server;
    private final Lazy<List<String>> memoizedAvCommands = new Lazy<>(this::loadAvailableCommands);
}