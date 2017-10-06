package org.zalando.riptide;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;

public final class AnyDispatchTest {

    private final URI url = URI.create("http://localhost");

    private final Http unit;
    private final MockRestServiceServer server;

    public AnyDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    public void shouldDispatchAny() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final AtomicReference<ClientHttpResponse> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(status(),
                        on(CREATED).call(pass()),
                        anyStatus().call(ClientHttpResponse.class, capture::set))
                .join();

        final ClientHttpResponse response = capture.get();
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getHeaders().getContentType(), is(APPLICATION_JSON));
    }

}
