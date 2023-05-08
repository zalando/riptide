package org.zalando.riptide.compression;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;

public class MockWebServerUtil {

    public static String getBaseUrl(MockWebServer server) {
        return String.format("http://%s:%s", server.getHostName(), server.getPort());
    }

    public static MockResponse emptyMockResponse() {
        return new MockResponse().setResponseCode(NO_CONTENT.value());
    }

    public static MockResponse emptyTextPlainMockResponse() {
        return new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody("")
                .setHeader("Content-Type", "text/plain");
    }
    public static void verify(MockWebServer server,
                              int expectedRequestsCount,
                              String expectedPath,
                              Consumer<Headers> headersVerifier) {
        assertEquals(expectedRequestsCount, server.getRequestCount());
        range(0, expectedRequestsCount).forEach(i -> {
            try {
                RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS);
                //TODO: add not null check to MockWebServerUtil
                assertNotNull(recordedRequest);
                assertEquals(expectedPath, recordedRequest.getPath());
                headersVerifier.accept(recordedRequest.getHeaders());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static RecordedRequest getRecorderRequest(MockWebServer server) {
        try {
            return server.takeRequest(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void verify(MockWebServer server,
                              int expectedRequestsCount,
                              String expectedPath) {
        verify(server, expectedRequestsCount, expectedPath, headers -> {
        });
    }

    public static void verify(MockWebServer server, String... expectedPaths) {

        assertEquals(expectedPaths.length, server.getRequestCount());
        for (String expectedPath : expectedPaths) {
            try {
                RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS);
                assertEquals(expectedPath, recordedRequest.getPath());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String readResourceAsString(String resourceName) throws IOException {
        try (var inputStream = getResource(resourceName).openStream()) {
            return new String(inputStream.readAllBytes(), UTF_8);
        }
    }
}
