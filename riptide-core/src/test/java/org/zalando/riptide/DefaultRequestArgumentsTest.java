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

    private final DefaultRequestArguments unit = new DefaultRequestArguments();

    @Value
    public static final class Assertion<T> {
        BiFunction<DefaultRequestArguments, T, DefaultRequestArguments> wither;
        T argument;
        Function<DefaultRequestArguments, T> getter;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new Assertion<>(DefaultRequestArguments::withBaseUrl, URI.create("https://api.example.com"), DefaultRequestArguments::getBaseUrl)},
                {new Assertion<>(DefaultRequestArguments::withMethod, HttpMethod.GET, DefaultRequestArguments::getMethod)},
                {new Assertion<>(DefaultRequestArguments::withUriTemplate, "/{id}", DefaultRequestArguments::getUriTemplate)},
                {new Assertion<>(DefaultRequestArguments::withUriVariables, ImmutableList.of(123), DefaultRequestArguments::getUriVariables)},
                {new Assertion<>(DefaultRequestArguments::withUri, URI.create("/123"), DefaultRequestArguments::getUri)},
                {new Assertion<>(DefaultRequestArguments::withQueryParams, ImmutableMultimap.of("k", "v"), DefaultRequestArguments::getQueryParams)},
                {new Assertion<>(DefaultRequestArguments::withRequestUri, URI.create("https://api.example.com/123?k=v"), DefaultRequestArguments::getRequestUri)},
                {new Assertion<>(DefaultRequestArguments::withHeaders, ImmutableMultimap.of("Secret", "true"), DefaultRequestArguments::getHeaders)},
                {new Assertion<>(DefaultRequestArguments::withBody, new Object(), DefaultRequestArguments::getBody)},
        });
    }

    @Test
    public void shouldOptimizeForReapplyingSameValue() {
        final DefaultRequestArguments applied = assertion.wither.apply(unit, assertion.argument);
        final DefaultRequestArguments appliedAgain = assertion.wither.apply(applied, assertion.argument);

        assertThat(appliedAgain, is(sameInstance(applied)));
    }

    @Test
    public void shouldModifyValue() {
        final DefaultRequestArguments applied = assertion.wither.apply(unit, assertion.argument);

        assertThat(applied, is(not(sameInstance(unit))));
        assertThat(assertion.getter.apply(applied), is(sameInstance(assertion.argument)));
    }

}