package org.zalando.riptide.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class StreamFilter extends FilterInputStream {

    private static final int JSON_SEQUENCE_RECORD_SEPARATOR = 30;

    final int size;

    protected StreamFilter(final InputStream in) {
        this(in, 8192);
    }

    protected StreamFilter(final InputStream in, final int size) {
        super(in);
        this.size = size;
    }

    private boolean filtered(final byte read) {
        return read == JSON_SEQUENCE_RECORD_SEPARATOR;
    }

    @Override
    public int read() throws IOException {
        while (true) {
            final int read = super.read();
            if (read == -1) {
                return -1;
            } else if (!filtered((byte) read)) {
                return read;
            }
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = super.read(b, off, len);
        if (read == -1) {
            return -1;
        }
        final int until = off + read;
        int last = off;
        for (int index = off; index < until; index++) {
            if (!filtered(b[index])) {
                if (index != last) {
                    b[last] = b[index];
                }
                last++;
            }
        }
        for (int index = last; index < until; index++) {
            b[index] = 0;
        }
        return last - off;
    }

    @Override
    public long skip(final long n) throws IOException {
        long sum = 0;
        final byte[] b = new byte[size];
        while (sum < n) {
            final int left = (int) (n - sum);
            final int len = left > size ? size : left;
            final int read = read(b, 0, len);
            if (read == -1) {
                return sum;
            }
            sum += read;
        }
        return sum;
    }

}