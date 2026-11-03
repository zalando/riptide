package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.UrlResolution.APPEND;

final class UrlResolutionTest {

    static Stream<Arguments> appendCases() {
        return Stream.of(
                Arguments.of(
                        "https://example.com/orders?tenant=india&apiKey=abc123",
                        "?page=2",
                        "https://example.com/orders?tenant=india&apiKey=abc123&page=2"),
                Arguments.of(
                        "https://example.com/api?tenant=india",
                        "orders?page=2",
                        "https://example.com/api/orders?tenant=india&page=2"),
                Arguments.of(
                        "https://example.com/orders?tag=red&tag=blue",
                        "?page=2",
                        "https://example.com/orders?tag=red&tag=blue&page=2"),
                Arguments.of(
                        "https://example.com/orders?tenant=india",
                        "orders",
                        "https://example.com/orders/orders?tenant=india"),
                Arguments.of(
                        "https://example.com/orders",
                        "?page=2",
                        "https://example.com/orders?page=2"));
    }

    @ParameterizedTest
    @MethodSource("appendCases")
    void shouldAppendPathAndMergeQueryParameters(
            final String baseUrl, final String uri, final String expected) {
        assertThat(APPEND.resolve(URI.create(baseUrl), URI.create(uri)), is(URI.create(expected)));
    }
}
