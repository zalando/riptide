package org.zalando.riptide.httpclient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class EndOfStreamAwareInputStream extends FilterInputStream {

    @FunctionalInterface
    interface Closer {
        void close(InputStream original, boolean endOfStreamDetected) throws IOException;
    }

    private final Closer closer;
    private boolean endOfStreamDetected;

    EndOfStreamAwareInputStream(final InputStream in, final Closer closer) {
        super(in);
        this.closer = closer;
    }

    @Override
    public int read() throws IOException {
        return detectEndOfStream(super.read());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return detectEndOfStream(super.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return detectEndOfStream(super.read(b, off, len));
    }

    private int detectEndOfStream(final int read) {
        if (read == -1) {
            endOfStreamDetected = true;
        }

        return read;
    }

    @Override
    public void close() throws IOException {
        closer.close(in, endOfStreamDetected);
    }

}
