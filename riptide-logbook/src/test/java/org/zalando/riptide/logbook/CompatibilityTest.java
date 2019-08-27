package org.zalando.riptide.logbook;

import lombok.*;
import org.junit.jupiter.api.*;
import org.zalando.logbook.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

interface CompatibilityTest {

    @Test
    default void unbufferedWithoutUnbufferedBufferPassing() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public HttpRequest process(final HttpRequest request) {
                return request.withoutBody();
            }

            @Override
            public HttpResponse process(final HttpRequest request, final HttpResponse response) {
                return response.withoutBody();
            }
        });

        verifyWithoutBody(interaction.getRequest());
        verifyWithoutBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithOfferingBufferBuffering() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public HttpRequest process(final HttpRequest request) throws IOException {
                return request.withBody();
            }

            @Override
            public HttpResponse process(final HttpRequest request, final HttpResponse response) throws IOException {
                return response.withBody();
            }
        });

        verifyWithBody(interaction.getRequest());
        verifyWithBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithoutUnbufferedBufferPassingWithPassing() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public HttpRequest process(final HttpRequest request) {
                return request.withoutBody();
            }

            public void write(final Precorrelation precorrelation, final HttpRequest request,
                    final Sink sink) throws IOException {
                request.withBody();
                sink.write(precorrelation, request);
            }

            @Override
            public HttpResponse process(final HttpRequest request, final HttpResponse response) {
                return response.withoutBody();
            }

            public void write(final Correlation correlation, final HttpRequest request, final HttpResponse response,
                    final Sink sink) throws IOException {
                response.withBody();
                sink.write(correlation, request, response);
            }
        });

        verifyWithoutBody(interaction.getRequest());

        // TODO this means we are actually not in Passing, but in Offering
        verifyWithBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithOfferingWithoutUnbufferedBufferPassing() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public HttpRequest process(final HttpRequest request) throws IOException {
                return request.withBody().withoutBody();
            }

            @Override
            public HttpResponse process(final HttpRequest request, final HttpResponse response) throws IOException {
                return response.withBody().withoutBody();
            }
        });

        verifyWithoutBody(interaction.getRequest());
        verifyWithoutBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithOfferingBufferBufferingWithBuffering() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public void write(final Precorrelation precorrelation, final HttpRequest request,
                    final Sink sink) throws IOException {
                request.withBody();
                sink.write(precorrelation, request);
            }

            @Override
            public void write(final Correlation correlation, final HttpRequest request, final HttpResponse response,
                    final Sink sink) throws IOException {
                response.withBody();
                sink.write(correlation, request, response);
            }
        });

        verifyWithBody(interaction.getRequest());
        verifyWithBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithOfferingBufferBufferingWithoutIgnoring() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public void write(final Precorrelation precorrelation, final HttpRequest request,
                    final Sink sink) throws IOException {
                request.withoutBody();
                sink.write(precorrelation, request);
            }

            @Override
            public void write(final Correlation correlation, final HttpRequest request, final HttpResponse response,
                    final Sink sink) throws IOException {
                response.withoutBody();
                sink.write(correlation, request, response);
            }
        });

        verifyWithoutBody(interaction.getRequest());
        verifyWithoutBody(interaction.getResponse());
    }

    @Test
    default void unbufferedWithOfferingBufferBufferingWithoutIgnoringWithBuffering() throws IOException {
        final Interaction interaction = test(new Strategy() {
            @Override
            public void write(final Precorrelation precorrelation, final HttpRequest request,
                    final Sink sink) throws IOException {
                request.getBody();
                request.withoutBody().withBody();
                sink.write(precorrelation, request);
            }

            @Override
            public void write(final Correlation correlation, final HttpRequest request, final HttpResponse response,
                    final Sink sink) throws IOException {
                response.getBody();
                response.withoutBody().withBody();
                sink.write(correlation, request, response);
            }
        });

        verifyWithBody(interaction.getRequest());
        verifyWithBody(interaction.getResponse());
    }

    @Value
    final class Interaction {
        HttpRequest request;
        HttpResponse response;
    }

    Interaction test(Strategy strategy) throws IOException;

    default void verifyWithBody(final HttpRequest request) throws IOException {
        assertNotEquals("", request.getBodyAsString());
    }

    default void verifyWithBody(final HttpResponse response) throws IOException {
        assertNotEquals("", response.getBodyAsString());
    }

    default void verifyWithoutBody(final HttpRequest request) throws IOException {
        assertEquals("", request.getBodyAsString());
    }

    default void verifyWithoutBody(final HttpResponse response) throws IOException {
        assertEquals("", response.getBodyAsString());
    }

}
