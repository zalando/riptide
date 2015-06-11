package org.example.application;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.Rest;

import java.net.URI;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public class ApiTest {

    private final URI url = URI.create("http://localhost");

    private MockRestServiceServer server;
    private Rest unit;

    @Before
    public void setUp() {
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        server = MockRestServiceServer.createServer(template);
        unit = Rest.create(template);
    }

    @Test
    public void shouldAllowExampleApiUsage() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        unit.execute(HttpMethod.GET, url).dispatch(status(),
                on(HttpStatus.CREATED).call(this::callback),
                on(HttpStatus.ACCEPTED).call(this::callback),
                on(HttpStatus.BAD_REQUEST).call(this::callback),
                anyStatus().call(this::callback));
    }

    private void callback(ClientHttpResponse response) {
        // no operation
    }

}
