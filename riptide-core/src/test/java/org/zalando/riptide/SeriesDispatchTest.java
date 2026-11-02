package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;

final class SeriesDispatchTest {

    @ParameterizedTest
    @CsvSource({
            "100, INFORMATIONAL",
            "199, INFORMATIONAL",
            "200, SUCCESSFUL",
            "299, SUCCESSFUL",
            "300, REDIRECTION",
            "399, REDIRECTION",
            "499, CLIENT_ERROR",
            "400, CLIENT_ERROR",
            "500, SERVER_ERROR",
            "599, SERVER_ERROR",
    })
    void shouldDispatch(final int status, final HttpStatus.Series expected) {
        final MockSetup setup = new MockSetup();
        final MockRestServiceServer server = setup.getServer();
        final Http unit = setup.getHttp();

        final URI url = URI.create("https://api.example.com");

        server.expect(requestTo(url)).andRespond(withRawStatus(status));

        unit.get(url)
                .dispatch(series(),
                        on(expected).call(response -> assertThat(HttpStatus.Series.valueOf(response.getStatusCode().value()), is(expected))),
                        anySeries().call(response -> fail("Unexpected fallback route")))
                .join();
    }
}
