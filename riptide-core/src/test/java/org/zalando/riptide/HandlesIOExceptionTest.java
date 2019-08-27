package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.http.client.*;

import java.io.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

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
