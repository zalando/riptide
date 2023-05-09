package org.zalando.riptide.auth;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

public class MockWebServerUtil {

    public static String getBaseUrl(MockWebServer server) {
        return String.format("http://%s:%s", server.getHostName(), server.getPort());
    }

    public static MockResponse emptyMockResponse() {
        return new MockResponse().setResponseCode(NO_CONTENT.value());
    }

    public static MockResponse jsonMockResponse(String body) {
        return new MockResponse().setResponseCode(OK.value())
                .setBody(body)
                .setHeader(CONTENT_TYPE, "application/json");
    }

    public static MockResponse jsonMockResponseFromResource(String resourceName) throws IOException {
        return new MockResponse().setResponseCode(OK.value())
                .setBody(readResourceAsString(resourceName))
                .setHeader(CONTENT_TYPE, "application/json");
    }

    public static MockResponse textMockResponse(String body) {
        return new MockResponse()
                .setResponseCode(OK.value())
                .setBody(body)
                .setHeader(CONTENT_TYPE, "text/plain");
    }

    public static RecordedRequest getRecordedRequest(MockWebServer server) {
        try {
            return server.takeRequest(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void verify(MockWebServer server,
                              int expectedRequestsCount,
                              String expectedPath,
                              String expectedMethod) {
        verify(server, expectedRequestsCount, expectedPath, expectedMethod, headers -> {
        });
    }

    public static void verify(MockWebServer server,
                              int expectedRequestsCount,
                              String expectedPath) {
        verify(server, expectedRequestsCount, expectedPath, HttpMethod.GET.toString(), headers -> {
        });
    }

    public static void verify(MockWebServer server,
                              int expectedRequestsCount,
                              String expectedPath,
                              String expectedMethod,
                              Consumer<Headers> headersVerifier) {
        assertEquals(expectedRequestsCount, server.getRequestCount());
        range(0, expectedRequestsCount).forEach(i -> {
            RecordedRequest recordedRequest = getRecordedRequest(server);
            assertNotNull(recordedRequest);
            assertEquals(expectedPath, recordedRequest.getPath());
            assertEquals(expectedMethod, recordedRequest.getMethod());
            headersVerifier.accept(recordedRequest.getHeaders());
        });
    }

    public static void verify(MockWebServer server, String... expectedPaths) {

        assertEquals(expectedPaths.length, server.getRequestCount());
        for (String expectedPath : expectedPaths) {
            RecordedRequest recordedRequest = getRecordedRequest(server);
            assertNotNull(recordedRequest);
            assertEquals(expectedPath, recordedRequest.getPath());

        }
    }

    public static String readResourceAsString(String resourceName) throws IOException {
        try (var inputStream = getResource(resourceName).openStream()) {
            return new String(inputStream.readAllBytes(), UTF_8);
        }
    }
}
