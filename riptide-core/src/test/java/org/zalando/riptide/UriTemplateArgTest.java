package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Arrays;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class UriTemplateArgTest {

    static Iterable<Arguments> data() {
        return Arrays.asList(
                Arguments.of(
                        null,
                        "https://api.example.com/pages/{page}",
                        new Object[]{2},
                        "https://api.example.com/pages/2"
                ),
                Arguments.of(
                        "https://api.example.org/",
                        "/pages/{page}",
                        new Object[]{3},
                        "https://api.example.org/pages/3"
                ),
                Arguments.of(
                        "https://api.example.org/",
                        "https://api.example.com/pages/{page}",
                        new Object[]{4},
                        "https://api.example.com/pages/4"
                ),
                Arguments.of(
                        "https://api.example.org/books/",
                        "./pages/{page}",
                        new Object[]{5},
                        "https://api.example.org/books/pages/5"
                ),
                Arguments.of(
                        "https://api.example.org/books/",
                        "../pages/{page}",
                        new Object[]{6},
                        "https://api.example.org/pages/6"
                ));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldExpand(final String baseUrl, final String uriTemplate, final Object[] uriVariables,
            final String requestUrl) {

        final MockSetup setup = new MockSetup(baseUrl);
        final MockRestServiceServer server = setup.getServer();
        final Http unit = setup.getHttp();

        server.expect(requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        unit.get(uriTemplate, uriVariables)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        server.verify();
    }

}
