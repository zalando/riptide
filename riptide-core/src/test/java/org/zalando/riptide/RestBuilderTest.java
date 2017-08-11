package org.zalando.riptide;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Deprecated
public class RestBuilderTest {

    @Test
    public void shouldConfigure() {
        final HttpBuilder builder = spy(HttpBuilder.class);

        final RestConfigurer unit = RestBuilder.simpleRequestFactory(newDirectExecutorService());

        unit.configure(builder);

        final ArgumentCaptor<AsyncClientHttpRequestFactory> captor =
                ArgumentCaptor.forClass(AsyncClientHttpRequestFactory.class);

        verify(builder).requestFactory(captor.capture());
        final AsyncClientHttpRequestFactory factory = captor.getValue();

        assertThat(factory, is(instanceOf(SimpleClientHttpRequestFactory.class)));
    }

}
