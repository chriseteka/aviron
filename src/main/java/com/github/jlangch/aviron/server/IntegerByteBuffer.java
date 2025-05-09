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
