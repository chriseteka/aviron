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
package com.github.jlangch.aviron.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.util.Arrays;

import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.ex.CommunicationException;


public class ServerIO {

    public ServerIO(
            final String serverHostname, 
            final FileSeparator serverPlatform
    ) {
        this(serverHostname, DEFAULT_SERVER_PORT, serverPlatform);
    }

    public ServerIO(
            final String serverHostname, 
            final int serverPort, 
            final FileSeparator serverFileSeparator
    ) {
        this(serverHostname, serverPort, serverFileSeparator, 
        	 DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public ServerIO(
            final String serverHostname, 
            final int serverPort, 
            final FileSeparator serverFileSeparator,
            final int connectionTimeoutMillis,
            final int readTimeoutMillis
    ) {
        this.server = new InetSocketAddress(serverHostname, serverPort);
        this.serverFileSeparator = serverFileSeparator;
        this.connectionTimeout = connectionTimeoutMillis;
        this.readTimeout = readTimeoutMillis;
    }

    
    public String getHostname() {
        return server.getHostName();
    }

    public int getPort() {
        return server.getPort();
    }

    public FileSeparator getFileSeparator() {
        return serverFileSeparator;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeout;
    }

    public int getReadTimeoutMillis() {
        return readTimeout;
    }


    public String toServerPath(final Path path) {
        return serverFileSeparator.toServerPath(path);
    }

    public CommandRunDetails getLastCommandRunDetails() {
        return lastCommandRunDetails;
    }

    public boolean isReachable() {
        return isReachable(connectionTimeout);
    }

    public boolean isReachable(final int timeoutMillis) {
        try (SocketChannel socketChannel = SelectorProvider.provider().openSocketChannel()) {
            socketChannel.configureBlocking(true);
            socketChannel.socket().connect(server, timeoutMillis);
            return true;
        } 
        catch (SocketTimeoutException | ConnectException ex) {
            return false;
        } 
        catch (Exception ex) {
            return false;
        }
    }

    public String sendCommandAndReturnResponse(final String rawCommand) {
        final long start = System.currentTimeMillis();

        try (SocketChannel socketChannel = SocketChannel.open(server)) {
            if (readTimeout > 0) {
                socketChannel.socket().setSoTimeout(readTimeout);
            }

            final ByteBuffer command = ByteBuffer.wrap(rawCommand.getBytes(UTF_8));
            socketChannel.write(command);
            
            final String response = readResponse(socketChannel);

            final long elapsed = System.currentTimeMillis() - start;
            lastCommandRunDetails = new CommandRunDetails(rawCommand, response, elapsed);

            return response;
        } 
        catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    public String sendCommandWithDataAndReturnResponse(
            final String rawCommand, 
            final InputStream inputStream,
            final int chunkSize
    ) {
        // https://medium.com/aeturnuminc/virus-scanning-in-java-using-clamav-d60a181023df
        final long start = System.currentTimeMillis();

        try (SocketChannel socketChannel = SocketChannel.open(server)) {
            if (readTimeout > 0) {
                socketChannel.socket().setSoTimeout(readTimeout);
            }
            
            // send command
            final ByteBuffer command = ByteBuffer.wrap(rawCommand.getBytes(UTF_8));
            socketChannel.write(command);

            // send data as 1..n chunks
            final IntegerByteBuffer chunkSizeBuffer = new IntegerByteBuffer();
            final byte[] data = new byte[chunkSize];
            int readBytes;
            while ((readBytes = inputStream.read(data)) != -1) {
            	chunkSizeBuffer.putInt(readBytes);  
                socketChannel.write(chunkSizeBuffer.getByteBuffer());
                socketChannel.write(ByteBuffer.wrap(data, 0, readBytes));
            }

            // Send zero-length chunk to signal end of stream
            chunkSizeBuffer.putInt(0);
            socketChannel.write(chunkSizeBuffer.getByteBuffer());

            final String response = readResponse(socketChannel);

            final long elapsed = System.currentTimeMillis() - start;
            lastCommandRunDetails = new CommandRunDetails(rawCommand, response, elapsed);

            return response;
        }
        catch (IOException ex) {
            throw new CommunicationException(ex);
        }
    }

    private String readResponse(final SocketChannel socketChannel) throws IOException {
        final ByteBuffer readBuffer = ByteBuffer.allocate(32);
        byte[] responseBytes = new byte[0];

        int readSize = socketChannel.read(readBuffer);
        while (readSize > -1) {
            final byte[] readArray = Arrays.copyOf(readBuffer.array(), readSize);
            final byte[] combined = new byte[responseBytes.length + readArray.length];
            System.arraycopy(responseBytes, 0, combined, 0, responseBytes.length);
            System.arraycopy(readArray, 0, combined, responseBytes.length, readArray.length);
            responseBytes = combined;

            readBuffer.clear();
            readSize = socketChannel.read(readBuffer);
        }

        return new String(responseBytes, UTF_8);
    }


    public static final String LOCALHOST = "localhost";

    public static final int DEFAULT_SERVER_PORT = 3310;

    public static final int DEFAULT_CONNECTION_TIMEOUT = 3_000;
    public static final int DEFAULT_READ_TIMEOUT = 20_000;

    public static final FileSeparator DEFAULT_SERVER_FILESEPARATOR = FileSeparator.JVM_PLATFORM;


    private final InetSocketAddress server;
    private final FileSeparator serverFileSeparator;

    private final int connectionTimeout;
    private final int readTimeout;

    private CommandRunDetails lastCommandRunDetails;
}
