package org.zalando.riptide;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.RestBuilder.simpleRequestFactory;
import static org.zalando.riptide.Route.pass;

@RunWith(Parameterized.class)
public class NoBaseUrlUriTemplateTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Method method;
    private final Executor executor;

    private final ExecutorService threadPool = newSingleThreadExecutor();
    private final Rest unit = Rest.builder()
            .baseUrl((URI) null)
            .configure(simpleRequestFactory(threadPool))
            .converter(createJsonConverter())
            .build();

    private MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        return converter;
    }

    public NoBaseUrlUriTemplateTest(final Method method, final Executor executor) {
        this.method = method;
        this.executor = executor;
    }

    interface Executor {
        Requester execute(Rest client, String uriTemplate);
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {Method.GET, (Executor) Rest::get},
                {Method.HEAD, (Executor) Rest::head},
                {Method.POST, (Executor) Rest::post},
                {Method.PUT, (Executor) Rest::put},
                {Method.custom("PATCH"), (Executor) Rest::patch},
                {Method.DELETE, (Executor) Rest::delete},
                {Method.OPTIONS, (Executor) Rest::options},
                {Method.TRACE, (Executor) Rest::trace},
        });
    }

    @Test
    public void shouldResolveAbsolutePathAgainstBaseURL() {
        driver.addExpectation(onRequestTo("https://api.example.org/pages").withMethod(method),
                giveEmptyResponse());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("URI is not absolute");

        executor.execute(unit, "/pages")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldResolveRelativePathAgainstBaseURL() {
        driver.addExpectation(onRequestTo("https://api.example.org/api/pages").withMethod(method),
                giveEmptyResponse());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("URI is not absolute");

        executor.execute(unit, "pages")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
