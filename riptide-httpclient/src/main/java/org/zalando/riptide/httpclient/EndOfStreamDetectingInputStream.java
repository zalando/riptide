package org.zalando.riptide.httpclient;

import org.zalando.fauxpas.ThrowingBiConsumer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class EndOfStreamDetectingInputStream extends FilterInputStream {

    private boolean ended;
    private final ThrowingBiConsumer<InputStream, Boolean, IOException> closer;

    EndOfStreamDetectingInputStream(final InputStream in,
            final ThrowingBiConsumer<InputStream, Boolean, IOException> closer) {
        super(in);
        this.closer = closer;
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();

        if (read == -1) {
            ended = true;
        }

        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return onRead(super.read(b), b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return onRead(super.read(b, off, len), len);
    }

    private int onRead(final int read, final int max) {
        if (read < max) {
            ended = true;
        }

        return read;
    }

    @Override
    public void close() throws IOException {
        closer.tryAccept(in, ended);
    }

}
