package org.zalando.riptide;

import com.google.common.collect.LinkedHashMultimap;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.OffsetDateTime;

public class RequesterImmutabilityTest {

    private final Http unit;

    public RequesterImmutabilityTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
    }

    @Test
    public void shouldNotReturnSameInstanceAfterMutation() {
        Requester original = unit.get();
        Assert.assertNotSame(original, original.accept(MediaType.ALL));
        Assert.assertNotSame(original, original.contentType(MediaType.APPLICATION_JSON));
        Assert.assertNotSame(original, original.header("header","value"));
        Assert.assertNotSame(original, original.headers(new HttpHeaders()));
        Assert.assertNotSame(original, original.queryParam("p","v"));
        Assert.assertNotSame(original, original.queryParams(LinkedHashMultimap.create()));
        Assert.assertNotSame(original, original.ifMatch(""));
        Assert.assertNotSame(original, original.ifModifiedSince(OffsetDateTime.now()));
        Assert.assertNotSame(original, original.ifNoneMatch(""));
        Assert.assertNotSame(original, original.ifUnmodifiedSince(OffsetDateTime.now()));
    }
}
