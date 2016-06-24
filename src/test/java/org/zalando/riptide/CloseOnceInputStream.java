package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class CloseOnceInputStream extends InputStream {
    private final InputStream inputStream;
    private boolean isClosed;

    public CloseOnceInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public CloseOnceInputStream(final byte[] buf) {
        this(new ByteArrayInputStream(buf));
    }

    public CloseOnceInputStream(final byte[] buf, final int offset, final int length) {
        this(new ByteArrayInputStream(buf, offset, length));
    }

    public boolean isClosed() {
        return isClosed;
    }

    private void checkClosed() throws IOException {
        if (isClosed) {
            throw new IOException("Stream is already closed");
        }
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        isClosed = true;
        inputStream.close();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        checkClosed();
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public synchronized int read() throws IOException {
        checkClosed();
        return inputStream.read();
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        checkClosed();
        return inputStream.read(b, off, len);
    }

    @Override
    public synchronized long skip(final long n) throws IOException {
        checkClosed();
        return inputStream.skip(n);
    }

    @Override
    public synchronized int available() throws IOException {
        checkClosed();
        return inputStream.available();
    }
}
