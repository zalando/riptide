package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.test.web.client.*;

import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class AttributeTest {

    private final Http unit;
    private final MockRestServiceServer server;

    private final Attribute<String> attribute = Attribute.generate();

    AttributeTest() {
        final MockSetup setup = new MockSetup();
        this.server = setup.getServer();
        this.unit = setup.getHttpBuilder()
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            final String secret = arguments.getAttribute(attribute).orElse("unknown");
                            return execution.execute(arguments.withHeader("Secret", secret));
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
                .andExpect(header("Secret", "dXNlcjpzZWNyZXQK"))
                .andRespond(withSuccess());

        unit.trace("https://api.example.com")
                .attribute(attribute, "dXNlcjpzZWNyZXQK")
                .attribute(attribute, "dXNlcjpzZWNyZXQK")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

    @Test
    void shouldNotPassAttribute() {
        server.expect(requestTo("https://api.example.com"))
                .andExpect(header("Secret", "unknown"))
                .andRespond(withSuccess());

        unit.trace("https://api.example.com")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

}
