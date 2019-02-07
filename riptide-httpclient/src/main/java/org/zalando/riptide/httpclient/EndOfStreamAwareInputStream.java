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
        return detectEndOfStream(super.read(), 0);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return detectEndOfStream(super.read(b), b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return detectEndOfStream(super.read(b, off, len), len);
    }

    /**
     * Detects whether the end of the stream was reached by comparing {@code read} to the given {@code threshold}.
     * If less was read the end of the stream is detected and the {@link Closer closer} will be notified accordingly
     * upon calling {@link #close()}.
     *
     * @param read the read byte or the number of bytes read
     * @param threshold the threshold to compare {@code read} to
     * @return {@code read}
     */
    private int detectEndOfStream(final int read, final int threshold) {
        if (read < threshold) {
            endOfStreamDetected = true;
        }

        return read;
    }

    @Override
    public void close() throws IOException {
        closer.close(in, endOfStreamDetected);
    }

}
