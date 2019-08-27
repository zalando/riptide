package org.zalando.riptide;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.*;

import java.net.*;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;

final class ReasonPhraseDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    static List<Arguments> data() {
        return HttpStatuses.supported()
                .map(HttpStatus::getReasonPhrase)
                .map(Arguments::of)
                .collect(toList());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDispatch(final String expected) {
        final MockSetup setup = new MockSetup();
        final Http unit = setup.getHttp();
        final MockRestServiceServer server = setup.getServer();

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
