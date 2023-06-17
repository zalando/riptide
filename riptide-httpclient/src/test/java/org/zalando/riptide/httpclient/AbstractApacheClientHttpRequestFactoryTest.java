package org.zalando.riptide.httpclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zalando.fauxpas.ThrowingRunnable;
import org.zalando.riptide.Http;
import org.zalando.riptide.capture.Capture;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.Mode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Types.listOf;
import static org.zalando.riptide.httpclient.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.httpclient.MockWebServerUtil.jsonMockResponseFromResource;
import static org.zalando.riptide.httpclient.MockWebServerUtil.textMockResponse;
import static org.zalando.riptide.httpclient.MockWebServerUtil.verify;

public abstract class AbstractApacheClientHttpRequestFactoryTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(1)
                    .setMaxConnPerRoute(1)
                    .build())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(10)).build())
            .build();

    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client, getMode());

    abstract Mode getMode();

    private final Http http = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(getBaseUrl(server))
            .converter(new MappingJackson2HttpMessageConverter(createObjectMapper()))
            .build();

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @AfterEach
    void tearDown() throws IOException {
        factory.destroy();
        server.shutdown();
    }

    @Test
    void shouldReadContributors() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final RestTemplate template = new RestTemplate(factory);
        template.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter(createObjectMapper())));

        final List<User> users = template.exchange(getBaseUrl(server) + "/repos/zalando/riptide/contributors", GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<User>>() {
                }).getBody();

        Objects.requireNonNull(users);

        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    void shouldReadContributorsAsync() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final Capture<List<User>> capture = Capture.empty();

        final List<User> users = http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), capture))
                .thenApply(capture).join();

        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
        verify(server, 1, "/repos/zalando/riptide/contributors");
    }

    @Test
    void shouldReadContributorsManually() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final URI uri = URI.create(getBaseUrl(server)).resolve("/repos/zalando/riptide/contributors");
        final ClientHttpRequest request = factory.createRequest(uri, POST);

        request.getHeaders().setAccept(singletonList(APPLICATION_JSON));

        if (request instanceof StreamingHttpOutputMessage) {
            ((StreamingHttpOutputMessage) request)
                    .setBody(stream -> stream.write("{}".getBytes(UTF_8)));
        } else {
            request.getBody().write("{}".getBytes(UTF_8));
        }

        assertThat(request.getMethod(), is(POST));
        assertThat(request.getURI(), hasToString(endsWith("/repos/zalando/riptide/contributors")));
        assertThat(request.getHeaders().getAccept(), hasItem(APPLICATION_JSON));

        final ClientHttpResponse response = request.execute();

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getRawStatusCode(), is(200));
        assertThat(response.getStatusText(), is("OK"));
        assertThat(response.getHeaders(), is(not(anEmptyMap())));

        final InputStream stream = response.getBody();
        final ObjectMapper mapper = createObjectMapper();
        final List<User> users = mapper.readValue(stream, new TypeReference<List<User>>() {
        });
        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
        verify(server, 1, "/repos/zalando/riptide/contributors", POST.toString());
    }

    @Test
    void shouldReleaseConnection() {
        server.enqueue(textMockResponse("Hello world!"));
        server.enqueue(textMockResponse("Hello world!"));

        assertTimeout(Duration.ofMillis(750), () -> {
            http.get("/").call(pass()).join();
            http.get("/").call(pass()).join();
        });
        verify(server, 2, "/");
    }

    @Test
    void shouldReleaseConnectionOnFailureToReadBody() {
        server.enqueue(textMockResponse("[]"));
        server.enqueue(textMockResponse("[]"));

        final ThrowingRunnable<Throwable> request = throwingRunnable(() -> {
            try {
                http.get("/wrong-content-type")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(listOf(User.class), users -> {
                                }))
                        .join();
            } catch (final CompletionException e) {
                throw e.getCause();
            }
        });

        assertThat(assertThrows(RestClientException.class, request::run), instanceOf(RestClientException.class));
        assertThat(assertThrows(RestClientException.class, request::run), instanceOf(RestClientException.class));
        verify(server, 2, "/wrong-content-type");
    }

    @Test
    void shouldDestroyNonCloseableClient() throws IOException {
        new ApacheClientHttpRequestFactory(mock(HttpClient.class), getMode()).destroy();
    }

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        String getLogin() {
            return login;
        }
    }
}
