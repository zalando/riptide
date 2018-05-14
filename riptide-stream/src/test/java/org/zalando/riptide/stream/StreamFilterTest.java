package org.zalando.riptide.stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StreamFilterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFilterOnReadInteger() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream)) {
            final int filtered = 3;
            final int available = stream.available();
            int read = 0;
            while (unit.read() != -1) {
                read++;
            }
            assertThat(read, is(available - filtered));
        }
    }

    @Test
    public void shouldFilterOnReadBuffer() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream)) {
            final int filtered = 3;
            final int available = stream.available();
            int sum = 0;
            int read;
            final byte[] buffer = new byte[10];
            while ((read = unit.read(buffer, 0, buffer.length)) != -1) {
                sum += read;
            }
            assertThat(sum, is(available - filtered));
        }
    }

    @Test
    public void shouldFilterOnSkip() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream, 5)) {
            final long filtered = 3;
            final long available = stream.available();
            long sum = 0;
            long skipped;
            while ((skipped = unit.skip(10)) != 0) {
                sum += skipped;
            }
            assertThat(sum, is(available - filtered));
        }
    }
}
