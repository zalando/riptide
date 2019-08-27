package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.test.web.client.*;

import java.net.*;
import java.util.function.*;

import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class DynamicBaseUrlTest {

    private final Http unit;
    private final MockRestServiceServer server;

    @SuppressWarnings("unchecked")
    private final Supplier<URI> baseUrlProviderMock = mock(Supplier.class);

    DynamicBaseUrlTest() {
        final MockSetup setup = new MockSetup();

        this.unit = setup.getHttpBuilder().baseUrl(baseUrlProviderMock).build();
        this.server = setup.getServer();
    }

    @Test
    void shouldUseDynamicBaseUrl() {
        server.expect(requestTo("https://host1.example.com/123"))
                .andRespond(withSuccess());
        server.expect(requestTo("https://host2.example.com/123"))
                .andRespond(withSuccess());

        when(baseUrlProviderMock.get())
                .thenReturn(URI.create("https://host1.example.com"), URI.create("https://host2.example.com"));

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        server.verify();
        verify(baseUrlProviderMock, times(2)).get();
    }
}
