/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr303.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MarkableInputStream extends InputStream {
    private final InputStream delegate;
    private ByteArrayOutputStream baos;
    private byte[] buffer;
    private int bufferIndex;

    public MarkableInputStream(final InputStream stream) {
        delegate = stream;
        baos = new ByteArrayOutputStream();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(final int unused) {
        // no-op
    }

    @Override
    public void reset() throws IOException {
        if (baos == null) {
            throw new IOException("reset");
        }

        if (baos.size() == 0) {
            baos = null;
        } else {
            buffer = baos.toByteArray();
            baos = null;
            bufferIndex = 0;
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        if (baos != null) {
            baos.close();
        }
        baos = null;
        buffer = null;
    }

    @Override
    public int read() throws IOException {
        if (buffer != null) {
            return readFromBuffer();
        } else {
            return readFromStream();
        }
    }

    @Override
    public int read(final byte[] b, final int offset, final int length) throws IOException {
        if (buffer != null) {
            return readFromBuffer(b, offset, length);
        } else {
            return readFromStream(b, offset, length);
        }
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    private int readFromBuffer(final byte[] b, final int offset, final int length) {
        int bytesRead = -1;
        if (length <= buffer.length - bufferIndex) {
            System.arraycopy(buffer, bufferIndex, b, offset, length);
            bufferIndex += length;
            bytesRead = length;
        } else {
            int count = buffer.length - bufferIndex;
            System.arraycopy(buffer, bufferIndex, b, offset, count);
            buffer = null;
            bytesRead = count;
        }
        if (baos != null) {
            baos.write(b, offset, bytesRead);
        }
        return bytesRead;
    }

    private int readFromStream(final byte[] b, final int offset, final int length)
            throws IOException {

        int i = delegate.read(b, offset, length);
        if (i != -1 && baos != null) {
            baos.write(b, offset, i);
        }
        return i;
    }

    private int readFromBuffer() {
        int i = buffer[bufferIndex++];
        if (baos != null) {
            baos.write(i);
        }
        if (bufferIndex == buffer.length) {
            buffer = null;
        }
        return i;
    }

    private int readFromStream() throws IOException {
        int i = delegate.read();
        if (i != -1 && baos != null) {
            baos.write(i);
        }
        return i;
    }
}
