package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.INFORMATIONAL;
import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;

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
                assertThat(HttpStatus.resolve(response.getStatusCode().value()).series(), is(expected.series()));

        unit.get(url)
                .dispatch(series(),
                        on(INFORMATIONAL).call(verifier),
                        on(SUCCESSFUL).call(verifier),
                        on(REDIRECTION).call(verifier),
                        on(CLIENT_ERROR).call(verifier),
                        on(SERVER_ERROR).call(verifier));
    }

}
