package org.zalando.riptide;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.IF_MATCH;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.IF_NONE_MATCH;
import static com.google.common.net.HttpHeaders.IF_UNMODIFIED_SINCE;
import static java.lang.String.join;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.joining;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

@AllArgsConstructor
final class Requester extends AttributeStage {

    private final RequestExecution network;
    private final MessageReader reader;
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
    public HeaderStage headers(final Map<String, Collection<String>> headers) {
        return withArguments(arguments.withHeaders(headers));
    }

    private Requester withArguments(final RequestArguments arguments) {
        return new Requester(network, reader, arguments, plugins);
    }

    @Override
    public CompletableFuture<ClientHttpResponse> call(final Route route) {
        return body(null).call(route);
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
            final Plugin plugin = Plugin.composite(new DispatchPlugin(route, reader), plugins);

            // TODO update request uri after each plugin?
            // TODO wrap each request execution to catch any exceptions directly thrown by plugins and wrap them?
            final RequestExecution execution =
                    plugin.aroundAsync(
                            plugin.aroundDispatch(
                                    plugin.aroundNetwork(
                                            network)));

            // build request URI once so plugins can observe them once after in case they modify something
            return throwingFunction(execution::execute).apply(arguments.withRequestUri());
        }

    }

}
