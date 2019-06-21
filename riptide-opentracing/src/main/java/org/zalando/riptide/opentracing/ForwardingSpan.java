package org.zalando.riptide.opentracing;

import com.google.common.collect.ForwardingObject;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
abstract class ForwardingSpan extends ForwardingObject implements Span {

    @Override
    protected abstract Span delegate();

    @Override
    public SpanContext context() {
        return delegate().context();
    }

    @Override
    public Span setTag(final String key, final String value) {
        return delegate().setTag(key, value);
    }

    @Override
    public Span setTag(final String key, final boolean value) {
        return delegate().setTag(key, value);
    }

    @Override
    public Span setTag(final String key, final Number value) {
        return delegate().setTag(key, value);
    }

    @Override
    public <T> Span setTag(final Tag<T> tag, final T value) {
        return delegate().setTag(tag, value);
    }

    @Override
    public Span log(final Map<String, ?> fields) {
        return delegate().log(fields);
    }

    @Override
    public Span log(final long timestampMicroseconds, final Map<String, ?> fields) {
        return delegate().log(timestampMicroseconds, fields);
    }

    @Override
    public Span log(final String event) {
        return delegate().log(event);
    }

    @Override
    public Span log(final long timestampMicroseconds, final String event) {
        return delegate().log(timestampMicroseconds, event);
    }

    @Override
    public Span setBaggageItem(final String key, final String value) {
        return delegate().setBaggageItem(key, value);
    }

    @Override
    public String getBaggageItem(final String key) {
        return delegate().getBaggageItem(key);
    }

    @Override
    public Span setOperationName(final String operationName) {
        return delegate().setOperationName(operationName);
    }

    @Override
    public void finish() {
        delegate().finish();
    }

    @Override
    public void finish(final long finishMicros) {
        delegate().finish(finishMicros);
    }

}
