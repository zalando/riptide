package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.AsyncClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public final class HandlesIOExceptionTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldHandleExceptionDuringRequestCreation() {
        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectMessage("Could not create request");

        final AsyncClientHttpRequestFactory factory = (uri, httpMethod) -> {
            throw new IOException("Could not create request");
        };

        Http.builder().requestFactory(factory).defaultConverters().build()
                .get("http://localhost/")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

}
