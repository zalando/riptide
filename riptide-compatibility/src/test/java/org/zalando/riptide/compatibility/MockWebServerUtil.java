package org.zalando.riptide.compatibility;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.concurrent.TimeUnit;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
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
                .setHeader("Content-Type","application/json");
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

        assertEquals(expectedRequestsCount, server.getRequestCount());
        range(0, expectedRequestsCount).forEach(i -> {
            try {
                RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS);
                assertEquals(expectedPath, recordedRequest.getPath());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void verify(MockWebServer server, String... expectedPaths) {

        assertEquals(expectedPaths.length, server.getRequestCount());
        for (String expectedPath: expectedPaths) {
            try {
                RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS);
                assertEquals(expectedPath, recordedRequest.getPath());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
