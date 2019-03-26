package org.zalando.riptide;

import com.google.common.collect.Multimap;
import org.apiguardian.api.API;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public abstract class HeaderStage extends BodyStage {
    public abstract HeaderStage accept(MediaType acceptableMediaType, MediaType... acceptableMediaTypes);
    public abstract HeaderStage accept(Collection<MediaType> acceptableMediaTypes);
    public abstract HeaderStage contentType(MediaType contentType);
    public abstract HeaderStage ifModifiedSince(OffsetDateTime since);
    public abstract HeaderStage ifUnmodifiedSince(OffsetDateTime since);
    public abstract HeaderStage ifNoneMatch(String entityTag, String... entityTags);
    public abstract HeaderStage ifNoneMatch(Collection<String> entityTags);
    public abstract HeaderStage ifMatch(String entityTag, String... entityTags);
    public abstract HeaderStage ifMatch(Collection<String> entityTags);
    public abstract HeaderStage header(String name, String value);
    public abstract HeaderStage headers(Multimap<String, String> headers);
    public abstract HeaderStage headers(Map<String, ? extends Collection<String>> headers);
}
