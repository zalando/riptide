package org.zalando.riptide;

import com.google.common.collect.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.zalando.riptide.RequestArguments.*;

import javax.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.net.HttpHeaders.*;
import static java.lang.String.*;
import static java.time.ZoneOffset.*;
import static java.time.format.DateTimeFormatter.*;
import static java.util.stream.Collectors.*;
import static org.zalando.fauxpas.FauxPas.*;

@AllArgsConstructor
final class Requester extends AttributeStage {

    private final RequestExecution network;
    private final RequestArguments arguments;
    private final Plugin plugins;

    @Override
    public <T> AttributeStage attribute(final Attribute<T> attribute, final T value) {
        return withArguments(arguments.withAttribute(attribute, value));
    }

    @Override
    public QueryStage queryParam(final String name, final String value) {
        return withArguments(arguments.withQueryParam(name, value));
    }

    @Override
    public QueryStage queryParams(final Multimap<String, String> params) {
        return queryParams(params.asMap());
    }

    @Override
    public QueryStage queryParams(final Map<String, Collection<String>> params) {
        return withArguments(arguments.withQueryParams(params));
    }

    @Override
    public HeaderStage accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        return accept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
    }

    @Override
    public HeaderStage accept(final Collection<MediaType> acceptableMediaTypes) {
        return header(ACCEPT, acceptableMediaTypes.stream()
                .map(Objects::toString)
                .collect(joining(", ")));
    }

    @Override
    public HeaderStage contentType(final MediaType contentType) {
        return header(CONTENT_TYPE, contentType.toString());
    }

    @Override
    public HeaderStage ifModifiedSince(final OffsetDateTime since) {
        return header(IF_MODIFIED_SINCE, toHttpdate(since));
    }

    @Override
    public HeaderStage ifUnmodifiedSince(final OffsetDateTime since) {
        return header(IF_UNMODIFIED_SINCE, toHttpdate(since));
    }

    private String toHttpdate(final OffsetDateTime dateTime) {
        return RFC_1123_DATE_TIME.format(dateTime.atZoneSameInstant(UTC));
    }

    @Override
    public HeaderStage ifNoneMatch(final String entityTag, final String... entityTags) {
        return ifNoneMatch(Lists.asList(entityTag, entityTags));
    }

    @Override
    public HeaderStage ifNoneMatch(final Collection<String> entityTags) {
        return header(IF_NONE_MATCH, join(", ", entityTags));
    }

    @Override
    public HeaderStage ifMatch(final String entityTag, final String... entityTags) {
        return ifMatch(Lists.asList(entityTag, entityTags));
    }

    @Override
    public HeaderStage ifMatch(final Collection<String> entityTags) {
        return header(IF_MATCH, join(", ", entityTags));
    }

    @Override
    public HeaderStage header(final String name, final String value) {
        return withArguments(arguments.withHeader(name, value));
    }

    @Override
    public HeaderStage headers(final Multimap<String, String> headers) {
        return headers(headers.asMap());
    }

    @Override
    public HeaderStage headers(final Map<String, ? extends Collection<String>> headers) {
        return withArguments(arguments.withHeaders(headers));
    }

    private Requester withArguments(final RequestArguments arguments) {
        return new Requester(network, arguments, plugins);
    }

    @Override
    public CompletableFuture<ClientHttpResponse> call(final Route route) {
        return body(null).call(route);
    }

    @Override
    public DispatchStage body(@Nullable final Entity entity) {
        return new ResponseDispatcher(arguments.withEntity(entity));
    }

    @Override
    public <T> DispatchStage body(@Nullable final T body) {
        return new ResponseDispatcher(arguments.withBody(body));
    }

    @AllArgsConstructor
    private final class ResponseDispatcher extends DispatchStage {

        private final RequestArguments arguments;

        @Override
        public CompletableFuture<ClientHttpResponse> call(final Route route) {
            final RequestExecution execution =
                    plugins.aroundAsync(
                            plugins.aroundDispatch(
                                    plugins.aroundSerialization(
                                            plugins.aroundNetwork(
                                                    network))));

            return throwingFunction(execution::execute).apply(arguments.withRoute(route));
        }

    }

}
