package org.zalando.riptide.hystrix;

import com.google.common.collect.Iterables;
import com.hystrix.junit.HystrixRequestContextRule;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.HystrixRequestLog;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.Http;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public final class HystrixTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final HystrixRequestContextRule hystrix = new HystrixRequestContextRule();

    private final Http unit;
    private final MockRestServiceServer server;

    public HystrixTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRestBuilder().plugin(new HystrixPlugin()).build();
        this.server = setup.getServer();
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldExecute() {
        server.expect(requestTo("https://api.example.com/foo"))
                .andRespond(withSuccess().body("Hello!"));

        final AtomicReference<String> capture = new AtomicReference<>();

        unit.get("/foo")
                .dispatch(series(),
                        on(SUCCESSFUL).call(String.class, capture::set))
                .join();

        assertThat(capture.get(), is("Hello!"));
    }

    @Test
    public void shouldUseHostAsGroupKeyAndPathAsCommandKey() {
        server.expect(requestTo("https://api.example.com/foo?async=true"))
                .andRespond(withSuccess());

        unit.get("/foo")
                .queryParam("async", "true")
                .dispatch(series(), anySeries().call(pass())).join();

        final HystrixInvokableInfo<?> command = intercept();

        assertThat(command.getCommandGroup(), hasToString("api.example.com"));
        assertThat(command.getCommandKey(), hasToString("GET /foo"));
    }

    @Test
    public void shouldUseUriTemplateAsCommandKeyIfAvailable() {
        server.expect(requestTo("https://api.example.com/foo"))
                .andRespond(withSuccess());

        unit.get("/{id}", "foo")
                .dispatch(series(), anySeries().call(pass())).join();

        final HystrixInvokableInfo<?> command = intercept();

        assertThat(command.getCommandGroup(), hasToString("api.example.com"));
        assertThat(command.getCommandKey(), hasToString("GET /{id}"));
    }

    private HystrixInvokableInfo<?> intercept() {
        final HystrixRequestLog log = HystrixRequestLog.getCurrentRequest();
        final Collection<HystrixInvokableInfo<?>> commands = log.getAllExecutedCommands();

        assertThat(commands, hasSize(1));

        return Iterables.getOnlyElement(commands);
    }

}
