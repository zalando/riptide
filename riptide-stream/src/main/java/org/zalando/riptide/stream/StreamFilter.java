package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide: Stream
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class StreamFilter extends FilterInputStream {

    private static final int JSON_SEQUENCE_SEPARATOR = 30;

    final int size;

    protected StreamFilter(InputStream in) {
        this(in, 8192);
    }

    protected StreamFilter(InputStream in, int size) {
        super(in);
        this.size = size;
    }

    private boolean filtered(byte read) {
        return read == JSON_SEQUENCE_SEPARATOR;
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
    public int read(byte[] b, int off, int len) throws IOException {
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
    public long skip(long n) throws IOException {
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