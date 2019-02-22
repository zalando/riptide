package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

final class DefaultRequestArgumentsTest {

    private final RequestArguments unit = RequestArguments.create();

    @Value
    private static final class Assertion<T> {
        BiFunction<RequestArguments, T, RequestArguments> wither;
        T argument;
        Function<RequestArguments, T> getter;
    }

    static List<Assertion<?>> data() {
        return Arrays.asList(
                new Assertion<>(RequestArguments::withBaseUrl, URI.create("https://api.example.com"), RequestArguments::getBaseUrl),
                new Assertion<>(RequestArguments::withUrlResolution, UrlResolution.APPEND, RequestArguments::getUrlResolution),
                new Assertion<>(RequestArguments::withMethod, HttpMethod.GET, RequestArguments::getMethod),
                new Assertion<>(RequestArguments::withUriTemplate, "/{id}", RequestArguments::getUriTemplate),
                new Assertion<>(RequestArguments::withUriVariables, ImmutableList.of(123), RequestArguments::getUriVariables),
                new Assertion<>(RequestArguments::withUri, URI.create("/123"), RequestArguments::getUri),
                new Assertion<>(RequestArguments::withAttributes, ImmutableMap.of(Attribute.generate(), "foo"), RequestArguments::getAttributes),
                new Assertion<>(RequestArguments::withQueryParams, ImmutableMultimap.of("k", "v"), RequestArguments::getQueryParams),
                new Assertion<>(RequestArguments::withRequestUri, URI.create("https://api.example.com/123?k=v"), RequestArguments::getRequestUri),
                new Assertion<>(RequestArguments::withHeaders, ImmutableMultimap.of("Secret", "true"), RequestArguments::getHeaders),
                new Assertion<>(RequestArguments::withBody, new Object(), RequestArguments::getBody)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    <T> void shouldOptimizeForReapplyingSameValue(final Assertion<T> assertion) {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);
        final RequestArguments appliedAgain = assertion.wither.apply(applied, assertion.argument);

        assertThat(appliedAgain, is(sameInstance(applied)));
    }

    @ParameterizedTest
    @MethodSource("data")
    <T> void shouldModifyValue(final Assertion<T> assertion) {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);

        assertThat(applied, is(not(sameInstance(unit))));
        assertThat(assertion.getter.apply(applied), is(sameInstance(assertion.argument)));
    }

}
