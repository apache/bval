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
package org.apache.bval.jsr.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class IOs {
    private IOs() {
        // no-op
    }

    public static InputStream convertToMarkableInputStream(final InputStream stream) {
        if (stream == null) {
            return null;
        }

        // force ByteArrayOutputStream since we close the stream ATM
        /*if (stream.markSupported()) {
            return stream;
        } else {*/
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        /*}*/
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }
}
