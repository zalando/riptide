package org.zalando.riptide;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;


@RunWith(Parameterized.class)
public final class StatusDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Http unit;
    private final MockRestServiceServer server;

    private final HttpStatus expected;

    public StatusDispatchTest(final HttpStatus expected) {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return HttpStatuses.supported()
                .map(s -> new Object[]{s})
                .collect(toList());
    }

    @Test
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(expected));

        final ClientHttpResponseConsumer verifier = response ->
                assertThat(response.getStatusCode(), is(expected));

        @SuppressWarnings("unchecked")
        final Binding<HttpStatus>[] bindings = HttpStatuses.supported()
                .map(status -> on(status).call(verifier))
                .toArray(Binding[]::new);

        unit.get(url).dispatch(status(), bindings);
    }

}
