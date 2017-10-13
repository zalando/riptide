package org.zalando.riptide;

import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.URI;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public class DynamicBaseUrlTest {

    private final Http unit;
    private final MockRestServiceServer server;

    @SuppressWarnings("unchecked")
    private final Supplier<URI> baseUrlProviderMock = mock(Supplier.class);

    public DynamicBaseUrlTest() {
        final MockSetup setup = new MockSetup();

        this.unit = setup.getHttpBuilder().baseUrl(baseUrlProviderMock).build();
        this.server = setup.getServer();
    }

    @Test
    public void shouldUseDynamicBaseUrl() {
        server.expect(requestTo("https://host1.example.com/123"))
                .andRespond(withSuccess());
        server.expect(requestTo("https://host2.example.com/123"))
                .andRespond(withSuccess());

        when(baseUrlProviderMock.get())
                .thenReturn(URI.create("https://host1.example.com"), URI.create("https://host2.example.com"));

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        server.verify();
        verify(baseUrlProviderMock, times(2)).get();
    }
}
