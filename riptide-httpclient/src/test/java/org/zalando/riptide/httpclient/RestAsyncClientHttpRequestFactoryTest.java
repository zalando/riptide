package org.zalando.riptide.httpclient;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponseAsBytes;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.listOf;

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
    private final ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    private final AsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Rest rest = Rest.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .build();

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldReadContributors() throws IOException, ExecutionException, InterruptedException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final AtomicReference<List<User>> reference = new AtomicReference<>();

        rest.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), reference::set)).get();

        final List<String> users = reference.get().stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    public void shouldReadContributorsManually() throws IOException, ExecutionException, InterruptedException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors").withMethod(Method.POST),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final URI uri = URI.create(driver.getBaseUrl()).resolve("/repos/zalando/riptide/contributors");
        final AsyncClientHttpRequest request = factory.createAsyncRequest(uri, POST);

        request.getHeaders().setAccept(Collections.singletonList(APPLICATION_JSON));
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
        final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        final List<User> users = mapper.readValue(stream, new TypeReference<List<User>>() { });
        final List<String> names = users.stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(names, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

}
