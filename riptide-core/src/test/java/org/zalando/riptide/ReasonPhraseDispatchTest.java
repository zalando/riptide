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
import static org.zalando.riptide.Navigators.reasonPhrase;


@RunWith(Parameterized.class)
public final class ReasonPhraseDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Http unit;
    private final MockRestServiceServer server;

    private final String expected;

    public ReasonPhraseDispatchTest(final String expected) {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return HttpStatuses.supported()
                .map(HttpStatus::getReasonPhrase)
                .map(s -> new Object[]{s})
                .collect(toList());
    }

    @Test
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatuses.supported()
                .filter(s -> s.getReasonPhrase().equals(expected))
                .findFirst().get()));

        final ClientHttpResponseConsumer verifier = response ->
                assertThat(response.getStatusText(), is(expected));

        @SuppressWarnings("unchecked")
        final Binding<String>[] bindings = HttpStatuses.supported()
                .map(HttpStatus::getReasonPhrase)
                .map(reasonPhrase -> on(reasonPhrase).call(verifier))
                .toArray(Binding[]::new);

        unit.get(url).dispatch(reasonPhrase(), bindings);
    }

}
