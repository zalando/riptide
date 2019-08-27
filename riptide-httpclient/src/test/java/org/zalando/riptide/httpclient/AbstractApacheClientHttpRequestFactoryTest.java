package org.zalando.riptide.httpclient;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.core.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.capture.*;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static com.google.common.io.Resources.*;
import static java.nio.charset.StandardCharsets.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.Types.*;

public abstract class AbstractApacheClientHttpRequestFactoryTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setMaxConnTotal(1)
            .setMaxConnPerRoute(1)
            .build();

    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client, getMode());

    abstract Mode getMode();

    private final Http http = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .converter(new MappingJackson2HttpMessageConverter(createObjectMapper()))
            .build();

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @AfterEach
    void tearDown() throws IOException {
        factory.destroy();
    }

    @Test
    void shouldReadContributors() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final RestTemplate template = new RestTemplate(factory);
        template.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter(createObjectMapper())));

        final List<User> users = template.exchange(driver.getBaseUrl() + "/repos/zalando/riptide/contributors", GET,
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
    void shouldReadContributorsManually() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors").withMethod(ClientDriverRequest.Method.POST),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final URI uri = URI.create(driver.getBaseUrl()).resolve("/repos/zalando/riptide/contributors");
        final ClientHttpRequest request = factory.createRequest(uri, POST);

        request.getHeaders().setAccept(singletonList(APPLICATION_JSON));

        if (request instanceof StreamingHttpOutputMessage) {
            ((StreamingHttpOutputMessage) request)
                    .setBody(stream -> stream.write("{}".getBytes(UTF_8)));
        } else {
            request.getBody().write("{}".getBytes(UTF_8));
        }

        assertThat(request.getMethod(), is(POST));
        assertThat(request.getMethodValue(), is("POST"));
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
    }

    @Test
    void shouldReleaseConnection() {
        driver.addExpectation(onRequestTo("/"), giveResponse("Hello world!", "text/plain"));
        driver.addExpectation(onRequestTo("/"), giveResponse("Hello world!", "text/plain"));

        assertTimeout(Duration.ofMillis(750), () -> {
            http.get("/").call(pass()).join();
            http.get("/").call(pass()).join();
        });
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
