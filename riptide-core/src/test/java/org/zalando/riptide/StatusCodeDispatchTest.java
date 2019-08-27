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

final class StatusCodeDispatchTest {

    static List<Arguments> data() {
        return HttpStatuses.supported()
                .map(HttpStatus::value)
                .map(Arguments::of)
                .collect(toList());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDispatch(final int expected) {
        final MockSetup setup = new MockSetup();
        final MockRestServiceServer server = setup.getServer();
        final Http unit = setup.getHttp();

        final URI url = URI.create("https://api.example.com");

        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.valueOf(expected)));

        final ClientHttpResponseConsumer verifier = response ->
                assertThat(response.getRawStatusCode(), is(expected));

        @SuppressWarnings("unchecked")
        final Binding<Integer>[] bindings = HttpStatuses.supported()
                .map(HttpStatus::value)
                .map(status -> on(status).call(verifier))
                .toArray(Binding[]::new);

        unit.get(url).dispatch(statusCode(), bindings);
    }

}
