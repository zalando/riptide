package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

@RunWith(Parameterized.class)
public class NoBaseUrlUriTemplateTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Rest unit;
    private final Executor executor;

    public NoBaseUrlUriTemplateTest(final Executor executor) {
        final MockSetup setup = new MockSetup(null);
        this.unit = setup.getRest();
        this.executor = executor;
    }

    interface Executor {
        Requester execute(Rest client, String uriTemplate);
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {(Executor) Rest::get},
                {(Executor) Rest::head},
                {(Executor) Rest::post},
                {(Executor) Rest::put},
                {(Executor) Rest::patch},
                {(Executor) Rest::delete},
                {(Executor) Rest::options},
                {(Executor) Rest::trace},
        });
    }

    @Test
    public void shouldResolveAbsolutePathAgainstBaseURL() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Request URI is not absolute");

        executor.execute(unit, "/pages")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldResolveRelativePathAgainstBaseURL() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Request URI is not absolute");

        executor.execute(unit, "pages")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
