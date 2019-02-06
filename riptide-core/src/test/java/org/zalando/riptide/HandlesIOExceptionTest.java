package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

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

        final ClientHttpRequestFactory factory = (uri, httpMethod) -> {
            throw new IOException("Could not create request");
        };

        final Http unit = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(factory)
                .defaultConverters()
                .build();

        unit.get("http://localhost/")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();
    }

}
