package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public abstract class HeaderStage extends BodyStage {
    public abstract HeaderStage accept(MediaType acceptableMediaType, MediaType... acceptableMediaTypes);
    public abstract HeaderStage contentType(MediaType contentType);
    public abstract HeaderStage ifModifiedSince(OffsetDateTime since);
    public abstract HeaderStage ifUnmodifiedSince(OffsetDateTime since);
    public abstract HeaderStage ifNoneMatch(String... entityTags);
    public abstract HeaderStage ifMatch(String... entityTags);
    public abstract HeaderStage header(String name, String value);
    public abstract HeaderStage headers(HttpHeaders headers);
}
