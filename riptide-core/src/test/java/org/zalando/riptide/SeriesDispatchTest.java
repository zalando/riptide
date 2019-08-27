package org.zalando.riptide;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.*;

import java.net.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;

final class SeriesDispatchTest {

    static List<Arguments> data() {
        return Stream.of(HttpStatus.Series.values())
                .map(series -> Stream.of(HttpStatus.values())
                        .filter(status -> status.series() == series)
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new))
                .map(Arguments::of)
                .collect(toList());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDispatch(final HttpStatus expected) {
        final MockSetup setup = new MockSetup();
        final MockRestServiceServer server = setup.getServer();
        final Http unit = setup.getHttp();

        final URI url = URI.create("https://api.example.com");

        server.expect(requestTo(url)).andRespond(withStatus(expected));

        final ClientHttpResponseConsumer verifier = response ->
                assertThat(response.getStatusCode().series(), is(expected.series()));

        unit.get(url)
                .dispatch(series(),
                        on(INFORMATIONAL).call(verifier),
                        on(SUCCESSFUL).call(verifier),
                        on(REDIRECTION).call(verifier),
                        on(CLIENT_ERROR).call(verifier),
                        on(SERVER_ERROR).call(verifier));
    }

}
