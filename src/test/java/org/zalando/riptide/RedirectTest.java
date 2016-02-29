package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;

public final class RedirectTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    public RedirectTest() {
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldFollowRedirect() {
        final URI originalUrl = URI.create("https://api.example.com/accounts/123");
        final URI redirectUrl = URI.create("https://api.example.org/accounts/123");

        server.expect(requestTo(originalUrl)).andRespond(
                withStatus(HttpStatus.MOVED_PERMANENTLY)
                        .location(redirectUrl));

        server.expect(requestTo(redirectUrl)).andRespond(
                withSuccess()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("123"));

        assertThat(send(originalUrl, "test"), is("123"));
    }

    private String send(URI url, String accountName) {
        return unit.execute(POST, url, accountName).dispatch(series(),
                on(SUCCESSFUL, String.class).capture(),
                on(REDIRECTION).map(response ->
                        follow(response, accountName)).capture())
                .to(String.class);
    }

    private String follow(final ClientHttpResponse response, final String accountName) {
        // log redirect follow
        return send(response.getHeaders().getLocation(), accountName);
    }

}
