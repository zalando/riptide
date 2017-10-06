package org.zalando.riptide;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

@RunWith(Parameterized.class)
public class UriTemplateArgTest {

    private final Http unit;
    private final MockRestServiceServer server;

    private final String uriTemplate;
    private final Object[] uriVariables;
    private final String requestUrl;

    public UriTemplateArgTest(final String baseUrl, final String uriTemplate, final Object[] uriVariables,
            final String requestUrl) {
        final MockSetup setup = new MockSetup(baseUrl);
        this.unit = setup.getHttp();
        this.server = setup.getServer();

        this.uriTemplate = uriTemplate;
        this.uriVariables = uriVariables;
        this.requestUrl = requestUrl;
    }

    @Parameterized.Parameters(name = "{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        null,
                        "https://api.example.com/pages/{page}",
                        new Object[]{2},
                        "https://api.example.com/pages/2",
                },
                {
                        "https://api.example.org/",
                        "/pages/{page}",
                        new Object[]{3},
                        "https://api.example.org/pages/3",
                },
                {
                        "https://api.example.org/",
                        "https://api.example.com/pages/{page}",
                        new Object[]{4},
                        "https://api.example.com/pages/4",
                },
                {
                        "https://api.example.org/books/",
                        "./pages/{page}",
                        new Object[]{5},
                        "https://api.example.org/books/pages/5",
                },
                {
                        "https://api.example.org/books/",
                        "../pages/{page}",
                        new Object[]{6},
                        "https://api.example.org/pages/6",
                }
        });
    }

    @After
    public void tearDown() {
        server.verify();
    }

    @Test
    public void shouldExpand() {
        server.expect(requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        this.unit.get(uriTemplate, uriVariables)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
