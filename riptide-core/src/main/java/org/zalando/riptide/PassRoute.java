package org.zalando.riptide;

import com.google.common.io.ByteStreams;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;

import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;
import static org.zalando.fauxpas.TryWith.tryWith;

@API(status = STABLE)
public final class PassRoute implements Route {

    private static final Route PASS = new PassRoute();

    private PassRoute() {

    }

    @Override
    public void execute(final ClientHttpResponse response, final MessageReader reader) throws IOException {
        tryWith(response, this::exhaust);
    }

    private void exhaust(final ClientHttpResponse response) throws IOException {
        // needed for spring versions prior to 4.3.14
        Optional.ofNullable(response.getBody())
                .ifPresent(throwingConsumer(ByteStreams::exhaust));
    }

    /**
     * Produces a {@link Route} that ignores the response by actively discarding the response body.
     *
     * <strong>Beware</strong> that this behavior will block forever on potentially endless response bodies
     * like streams.
     *
     * @return a {@link Route route} that doesn't execute any meaningful logic
     */
    public static Route pass() {
        return PASS;
    }

}
