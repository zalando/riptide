package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class PeerHostnameTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        final String host = arguments.getRequestUri().getHost();
        return singleton(Tag.of("peer.hostname", host));
    }

}
