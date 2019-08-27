package org.zalando.riptide;

import com.google.common.collect.*;
import org.apiguardian.api.*;
import org.springframework.http.*;

import java.time.*;
import java.util.*;

import static org.apiguardian.api.API.Status.*;

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
