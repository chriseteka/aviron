package com.github.jlangch.aviron.commands.scan;

import java.io.InputStream;

import com.github.jlangch.aviron.commands.CommandDef;
import com.github.jlangch.aviron.commands.CommandFormat;
import com.github.jlangch.aviron.ex.UnknownCommandException;
import com.github.jlangch.aviron.server.ServerIO;


/**
 * It is mandatory to prefix this command with n or z.
 *
 * <p>Scan a stream of data. The stream is sent to clamd in chunks, after INSTREAM, 
 * on the same socket on which the command was sent. This avoids the overhead of 
 * establishing new TCP connections and problems with NAT. The format of the chunk 
 * is: '{length}{data}' where {length} is the size of the following data in bytes 
 * expressed as a 4 byte unsigned integer in network byte order and {data} is the 
 * actual chunk. Streaming is terminated by sending a zero-length chunk. 
 * 
 * <p>Note: do not exceed StreamMaxLength as defined in clamd.conf, otherwise clamd 
 * will reply with INSTREAM size limit exceeded and close the connection. 
 */
public class InStream extends ScanCommand {

    public InStream(final InputStream inputStream) {
        this(inputStream, DEFAULT_CHUNK_SIZE);
    }

    public InStream(InputStream inputStream, int chunkSize) {
        this.inputStream = inputStream;
        this.chunkSize = chunkSize;
    }


    @Override
    public CommandDef getCommandDef() {
        return new CommandDef("INSTREAM", CommandFormat.NULL_CHAR);
    }

    @Override
    public ScanResult send(final ServerIO server) {
        final String rawResponse = server.sendCommandWithDataAndReturnResponse(
                                            rawCommand(),
                                            inputStream, 
                                            chunkSize);
 
        final String response = removeResponseTerminator(rawResponse);
        if ("UNKNOWN COMMAND".equals(response)) {
            throw new UnknownCommandException(getCommandString());
        }

        return parseResponse(response);
    }


    public static final int DEFAULT_CHUNK_SIZE = 2048;

    private final InputStream inputStream;
    private final int chunkSize;
}