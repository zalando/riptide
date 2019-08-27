package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;

import java.io.*;
import java.net.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class AnyDispatchTest {

    private final URI url = URI.create("http://localhost");

    private final Http unit;
    private final MockRestServiceServer server;

    AnyDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    void shouldDispatchAny() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));


        final ClientHttpResponse response = unit.get(url)
                .dispatch(status(),
                        on(CREATED).call(pass()),
                        anyStatus().call(pass()))
                .join();

        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getHeaders().getContentType(), is(APPLICATION_JSON));
    }

}
