package org.zalando.riptide.opentelemetry.span;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.zalando.riptide.RequestArguments;

import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CompositeSpanDecoratorTest {

    @Test
    void shouldPassDecoratedRequestToFollowingDecorator() {
        final RequestArguments arguments = RequestArguments.create()
                .withMethod(HttpMethod.GET)
                .withUri(URI.create("https://example.com"));

        final SpanDecorator firstDecorator = new SpanDecorator() {
            @Override
            public RequestArguments decorateRequest(final RequestArguments requestArguments) {
                return requestArguments.withHeader("X-First", "first-value");
            }
        };
        final SpanDecorator secondDecorator = new SpanDecorator() {
            @Override
            public RequestArguments decorateRequest(final RequestArguments requestArguments) {
                assertThat(requestArguments.getHeaders().get("X-First"), is(List.of("first-value")));
                return requestArguments.withHeader("X-Second", "second-value");
            }
        };

        final RequestArguments decoratedArguments =
                CompositeSpanDecorator.composite(firstDecorator, secondDecorator).decorateRequest(arguments);

        assertThat(decoratedArguments.getHeaders().get("X-First"), is(List.of("first-value")));
        assertThat(decoratedArguments.getHeaders().get("X-Second"), is(List.of("second-value")));
    }
}
