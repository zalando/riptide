package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ForwardingSpanTest {

    private final Span delegate = spy(Span.class);
    private final Span unit = new ForwardingSpan() {
        @Override
        protected Span delegate() {
            return delegate;
        }
    };

    @Test
    void context() {
        assertSame(delegate.context(), unit.context());
    }

    @Test
    void setTag() {
        unit.setTag(Tags.COMPONENT, "JUnit");
        verify(delegate).setTag(Tags.COMPONENT, "JUnit");
    }

    @Test
    void setTagString() {
        unit.setTag("component", "JUnit");
        verify(delegate).setTag("component", "JUnit");
    }

    @Test
    void setTagBoolean() {
        unit.setTag("error", true);
        verify(delegate).setTag("error", true);
    }

    @Test
    void setTagNumber() {
        unit.setTag("http.status_code", 200);
        verify(delegate).setTag("http.status_code", 200);
    }

    @Test
    void log() {
        unit.log("event");
        verify(delegate).log("event");
    }

    @Test
    void logMap() {
        unit.log(Collections.emptyMap());
        verify(delegate).log(Collections.emptyMap());
    }

    @Test
    void logTimestamp() {
        unit.log(0L, "event");
        verify(delegate).log(0L, "event");
    }

    @Test
    void logMapTimestamp() {
        unit.log(0L, Collections.emptyMap());
        verify(delegate).log(0L, Collections.emptyMap());
    }

    @Test
    void setBaggageItem() {
        unit.setBaggageItem("k", "v");
        verify(delegate).setBaggageItem("k", "v");
    }

    @Test
    void getBaggageItem() {
        unit.getBaggageItem("k");
        verify(delegate).getBaggageItem("k");
    }

    @Test
    void setOperationName() {
        unit.setOperationName("test");
        verify(delegate).setOperationName("test");
    }

    @Test
    void finish() {
        unit.finish();
        verify(delegate).finish();
    }

    @Test
    void finishTimestamp() {
        unit.finish(0L);
        verify(delegate).finish(0L);
    }

}
