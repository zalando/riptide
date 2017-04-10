package org.zalando.riptide;

import java.net.URI;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public class DynamicBaseUrlTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    @SuppressWarnings("unchecked")
    private final Supplier<URI> baseUrlProviderMock = mock(Supplier.class);

    public DynamicBaseUrlTest() {
        final MockSetup setup = new MockSetup();

        this.unit = setup.getRestBuilder().baseUrl(baseUrlProviderMock).build();
        this.server = setup.getServer();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        reset(baseUrlProviderMock);
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldUseDynamicBaseUrl() {
        expectRequestTo("https://host1.example.com/123");
        expectRequestTo("https://host2.example.com/123");

        when(baseUrlProviderMock.get())
                .thenReturn(URI.create("https://host1.example.com"), URI.create("https://host2.example.com"));

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        verify(baseUrlProviderMock, times(2)).get();
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
                .andRespond(withSuccess());
    }

}
