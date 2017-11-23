package org.zalando.riptide.httpclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.Http;
import org.zalando.riptide.capture.Capture;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponseAsBytes;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Types.listOf;

public final class RestAsyncClientHttpRequestFactoryTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        public String getLogin() {
            return login;
        }
    }

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http http = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .converter(createJsonConverter())
            .build();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(createObjectMapper());
        return converter;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldReadContributors() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final RestTemplate template = new RestTemplate(factory);
        template.setMessageConverters(singletonList(createJsonConverter()));

        final List<User> users = template.exchange(driver.getBaseUrl() + "/repos/zalando/riptide/contributors", GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<User>>() {
                }).getBody();

        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    public void shouldReadContributorsAsync() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final Capture<List<User>> capture = Capture.empty();

        final List<User> users = http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), capture))
                .thenApply(capture).join();

        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    public void shouldReadContributorsManually() throws IOException, ExecutionException, InterruptedException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors").withMethod(Method.POST),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final URI uri = URI.create(driver.getBaseUrl()).resolve("/repos/zalando/riptide/contributors");
        final AsyncClientHttpRequest request = factory.createAsyncRequest(uri, POST);

        request.getHeaders().setAccept(singletonList(APPLICATION_JSON));
        request.getBody().write("{}".getBytes(UTF_8));

        assertThat(request.getMethod(), is(POST));
        assertThat(request.getURI(), hasToString(endsWith("/repos/zalando/riptide/contributors")));
        assertThat(request.getHeaders().getAccept(), hasItem(APPLICATION_JSON));

        final ClientHttpResponse response = request.executeAsync().get();

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getRawStatusCode(), is(200));
        assertThat(response.getStatusText(), is("OK"));
        assertThat(response.getHeaders(), is(not(anEmptyMap())));

        final InputStream stream = response.getBody();
        final ObjectMapper mapper = createObjectMapper();
        final List<User> users = mapper.readValue(stream, new TypeReference<List<User>>() { });
        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

}
