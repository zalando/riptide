package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import lombok.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public final class DefaultRequestArgumentsTest<T> {

    @Parameter
    public Assertion<T> assertion;

    private final RequestArguments unit = RequestArguments.create();

    @Value
    public static final class Assertion<T> {
        BiFunction<RequestArguments, T, RequestArguments> wither;
        T argument;
        Function<RequestArguments, T> getter;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new Assertion<>(RequestArguments::withBaseUrl, URI.create("https://api.example.com"), RequestArguments::getBaseUrl)},
                {new Assertion<>(RequestArguments::withUrlResolution, UrlResolution.APPEND, RequestArguments::getUrlResolution)},
                {new Assertion<>(RequestArguments::withMethod, HttpMethod.GET, RequestArguments::getMethod)},
                {new Assertion<>(RequestArguments::withUriTemplate, "/{id}", RequestArguments::getUriTemplate)},
                {new Assertion<>(RequestArguments::withUriVariables, ImmutableList.of(123), RequestArguments::getUriVariables)},
                {new Assertion<>(RequestArguments::withUri, URI.create("/123"), RequestArguments::getUri)},
                {new Assertion<>(RequestArguments::withQueryParams, ImmutableMultimap.of("k", "v"), RequestArguments::getQueryParams)},
                {new Assertion<>(RequestArguments::withRequestUri, URI.create("https://api.example.com/123?k=v"), RequestArguments::getRequestUri)},
                {new Assertion<>(RequestArguments::withHeaders, ImmutableMultimap.of("Secret", "true"), RequestArguments::getHeaders)},
                {new Assertion<>(RequestArguments::withBody, new Object(), RequestArguments::getBody)},
        });
    }

    @Test
    public void shouldOptimizeForReapplyingSameValue() {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);
        final RequestArguments appliedAgain = assertion.wither.apply(applied, assertion.argument);

        assertThat(appliedAgain, is(sameInstance(applied)));
    }

    @Test
    public void shouldModifyValue() {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);

        assertThat(applied, is(not(sameInstance(unit))));
        assertThat(assertion.getter.apply(applied), is(sameInstance(assertion.argument)));
    }

}