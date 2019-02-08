package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.reasonPhrase;

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
