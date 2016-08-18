package org.zalando.riptide;

public interface TryWith {

    static void tryWith(final AutoCloseable closeable, final ThrowingRunnable consumer) throws Exception {
        try {
            consumer.run();
        } catch (Exception ex) {
            try {
                closeable.close();
            } catch (Exception cex) {
                ex.addSuppressed(cex);
            }
            throw ex;
        }
        closeable.close();
    }

    static <T> void tryWith(final AutoCloseable closeable, final ThrowingConsumer<T> consumer, final T input)
            throws Exception {
        try {
            consumer.accept(input);
        } catch (Exception ex) {
            try {
                closeable.close();
            } catch (Exception cex) {
                ex.addSuppressed(cex);
            }
            throw ex;
        }
        closeable.close();
    }
}
