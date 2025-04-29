package com.github.jlangch.aviron.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class IntegerByteBuffer {

    public IntegerByteBuffer() {
        this.buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
    }

    public void putInt(final int value) {
        buffer.clear();
        buffer.putInt(value);
        buffer.flip();
    }

    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    public byte[] getBytes() {
        return buffer.array();
    }

    final ByteBuffer buffer;
}
