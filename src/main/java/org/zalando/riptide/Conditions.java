package org.zalando.riptide;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class Conditions {

    public static <A> DispatchableCondition<A> on(A attribute) {
        throw new UnsupportedOperationException();
    }

    public static <A, I> CallableCondition<A, I> on(A attribute, Class<I> type) {
        throw new UnsupportedOperationException();
    }

    public static <A> AnyCondition<A> any(Class<A> type) {
        throw new UnsupportedOperationException();
    }

    public static AnyCondition<HttpStatus> anyStatusCode() {
        return any(HttpStatus.class);
    }

    public static AnyCondition<HttpStatus.Series> anySeries() {
        return any(HttpStatus.Series.class);
    }

    public static AnyCondition<MediaType> anyContentType() {
        return any(MediaType.class);
    }
}
