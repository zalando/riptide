package org.zalando.riptide.compression;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.Mode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.compression.MockWebServerUtil.*;

class RequestCompressionPluginTest {

    private final MockWebServer server = new MockWebServer();

    private final ExecutorService executor = newSingleThreadExecutor();

    @SneakyThrows
    @AfterEach
    void tearDown() {
        executor.shutdown();
        server.shutdown();
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldCompressRequestBody(final ClientHttpRequestFactory factory) {
        server.enqueue(textMockResponse(""));

        final Http http = buildHttp(factory, new RequestCompressionPlugin());
        http.post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecordedRequest(server);
        verifyRequest(recordedRequest, "/", "POST");
        verifyRequestBody(recordedRequest, "{}");
        assertEquals("gzip", recordedRequest.getHeaders().get("Content-Encoding"));

    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldNotCompressEmptyRequestBody(final ClientHttpRequestFactory factory) {
        server.enqueue(textMockResponse(""));

        final Http http = buildHttp(factory, new RequestCompressionPlugin());
        http.post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecordedRequest(server);
        verifyRequest(recordedRequest, "/", "POST");
        assertEquals("application/json", recordedRequest.getHeaders().get("Content-Type"));
        assertEquals("", recordedRequest.getBody().readString(UTF_8));
        assertNull(recordedRequest.getHeaders().get("Content-Encoding"));
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldCompressWithGivenAlgorithm(final ClientHttpRequestFactory factory) {
        server.enqueue(textMockResponse(""));

        final Http http = buildHttp(factory, new RequestCompressionPlugin(Compression.of("identity", it -> it)));
        http.post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .call(pass())
                .join();


        RecordedRequest recordedRequest = getRecordedRequest(server);
        verifyRequest(recordedRequest, "/", "POST");
        verifyRequestBody(recordedRequest, "{}");
        assertEquals("identity", recordedRequest.getHeaders().get("Content-Encoding"));
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldBackOffIfAlreadyEncoded(final ClientHttpRequestFactory factory) {
        server.enqueue(textMockResponse(""));

        final Http http = buildHttp(factory, new RequestCompressionPlugin());
        http.post("/")
                .header(CONTENT_ENCODING, "custom")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecordedRequest(server);
        verifyRequest(recordedRequest, "/", "POST");
        verifyRequestBody(recordedRequest, "{}");
        assertEquals("custom", recordedRequest.getHeaders().get("Content-Encoding"));
    }

    private static void verifyRequest(RecordedRequest recordedRequest,
                                      String expectedPath,
                                      String expectedMethod) {
        assertNotNull(recordedRequest);
        assertEquals(expectedPath, recordedRequest.getPath());
        assertEquals(expectedMethod, recordedRequest.getMethod());
    }

    private static void verifyRequestBody(RecordedRequest recordedRequest,
                                          String expectedBody) {
        assertEquals("application/json", recordedRequest.getHeaders().get("Content-Type"));
        assertEquals(expectedBody, decompressIfNeeded(recordedRequest.getBody()));
    }

    private static String decompressIfNeeded(Buffer body) {
        if (isCompressed(body)) {
            try (GZIPInputStream gis = new GZIPInputStream(body.inputStream())) {
                final StringBuilder outStr = new StringBuilder();
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, UTF_8));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    outStr.append(line);
                }
                return outStr.toString();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return body.readString(UTF_8);
        }
    }

    public static boolean isCompressed(Buffer body) {
        return (body.getByte(0) == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (body.getByte(1) == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    private Http buildHttp(final ClientHttpRequestFactory factory, final Plugin... plugins) {
        return Http.builder()
                .executor(executor)
                .requestFactory(factory)
                .baseUrl(getBaseUrl(server))
                .plugins(asList(plugins))
                .build();
    }

    static class RequestFactorySource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    new SimpleClientHttpRequestFactory(),
                    // new Netty4ClientHttpRequestFactory(), # broken, see #823
                    new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()),
                    new ApacheClientHttpRequestFactory(HttpClients.createDefault(), Mode.BUFFERING),
                    new ApacheClientHttpRequestFactory(HttpClients.createDefault(), Mode.STREAMING)
            ).map(Arguments::of);
        }
    }

}
