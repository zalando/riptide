package org.zalando.riptide;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.Arrays;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

@RunWith(Parameterized.class)
public class UrlResolutionTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpMethod method;
    private final Executor executor;

    public UrlResolutionTest(final HttpMethod method, final Executor executor) {
        final MockSetup setup = new MockSetup("https://api.example.com/api/");
        this.unit = setup.getRestBuilder().urlResolution(UrlResolution.LAX).build();
        this.server = setup.getServer();

        this.method = method;
        this.executor = executor;
    }

    interface Executor {
        Requester execute(Rest client, URI uri);
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {HttpMethod.GET, (Executor) Rest::get},
                {HttpMethod.HEAD, (Executor) Rest::head},
                {HttpMethod.POST, (Executor) Rest::post},
                {HttpMethod.PUT, (Executor) Rest::put},
                {HttpMethod.PATCH, (Executor) Rest::patch},
                {HttpMethod.DELETE, (Executor) Rest::delete},
                {HttpMethod.OPTIONS, (Executor) Rest::options},
                {HttpMethod.TRACE, (Executor) Rest::trace},
        });
    }

    @After
    public void tearDown() {
        server.verify();
    }

    @Test
    public void shouldResolveAbsoluteUrlAgainstBaseURL() {
        server.expect(requestTo("http://api.example.org/index.html"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        executor.execute(unit, URI.create("http://api.example.org/index.html"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldResolveAbsolutePathAgainstBaseURL() {
        server.expect(requestTo("https://api.example.com/api/pages"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        executor.execute(unit, URI.create("/pages"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldResolveRelativePathAgainstBaseURL() {
        server.expect(requestTo("https://api.example.com/api/pages"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        executor.execute(unit, URI.create("pages"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldPathWithQueryParametersAgainstBaseURL() {
        server.expect(requestTo("https://api.example.com/api/pages?archive"))
                .andExpect(method(method))
                .andRespond(withSuccess());

        executor.execute(unit, URI.create("/pages?archive"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
