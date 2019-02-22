package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
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

    private final Attribute<String> attribute = Attribute.generate();

    AttributeTest() {
        final MockSetup setup = new MockSetup();
        this.server = setup.getServer();
        this.unit = setup.getHttpBuilder()
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution beforeSend(final RequestExecution execution) {
                        return arguments -> {
                            final String secret = arguments.getAttribute(attribute).orElse("unknown");
                            return execution.execute(arguments.withHeaders(ImmutableMultimap.of("Secret", secret)));
                        };
                    }
                })
                .build();
    }

    @Test
    void shouldSendNoBody() {
        server.expect(requestTo("https://api.example.com"))
                .andExpect(header("Secret", "dXNlcjpzZWNyZXQK"))
                .andRespond(withSuccess());

        unit.trace("https://api.example.com")
                .attribute(attribute, "dXNlcjpzZWNyZXQK")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        server.verify();
    }

}
