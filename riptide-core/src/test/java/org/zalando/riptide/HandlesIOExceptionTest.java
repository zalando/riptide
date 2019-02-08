package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class HandlesIOExceptionTest {

    @Test
    void shouldHandleExceptionDuringRequestCreation() {
        final ClientHttpRequestFactory factory = (uri, httpMethod) -> {
            throw new IOException("Could not create request");
        };

        final Http unit = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(factory)
                .defaultConverters()
                .build();

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get("http://localhost/")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()))
                        .join());

        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
        assertThat(exception.getMessage(), containsString("Could not create request"));
    }

}
