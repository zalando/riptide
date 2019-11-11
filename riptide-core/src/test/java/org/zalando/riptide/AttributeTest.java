package org.zalando.riptide;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class AttributeTest {

    private final Http unit;
    private final MockRestServiceServer server;

    AttributeTest() {
        final MockSetup setup = new MockSetup();
        this.server = setup.getServer();
        this.unit = setup.getHttpBuilder()
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            final boolean idempotent = arguments
                                    .getAttribute(Attributes.IDEMPOTENT)
                                    .orElse(false);
                            return execution.execute(arguments
                                    .withHeader("Idempotent", String.valueOf(idempotent)));
                        };
                    }
                })
                .build();
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void shouldPassAttribute() {
        server.expect(requestTo("https://api.example.com"))
                .andExpect(header("Idempotent", "false"))
                .andRespond(withSuccess());

        unit.trace("https://api.example.com")
                .attribute(Attributes.IDEMPOTENT, true)
                .attribute(Attributes.IDEMPOTENT, false)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldNotPassAttribute() {
        server.expect(requestTo("https://api.example.com"))
                .andExpect(header("Idempotent", "false"))
                .andRespond(withSuccess());

        unit.trace("https://api.example.com")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

}
