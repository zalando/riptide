package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.ReflectionUtils;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;
import org.zalando.riptide.opentelemetry.span.CompositeSpanDecorator;
import org.zalando.riptide.opentelemetry.span.RetrySpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class OpenTelemetryPluginFactoryTest {

    private static final Field SPAN_DECORATOR_FIELD = ReflectionUtils.findField(OpenTelemetryPlugin.class, "spanDecorator");
    private static final Field DECORATORS_FIELD = ReflectionUtils.findField(CompositeSpanDecorator.class, "decorators");

    static {
        SPAN_DECORATOR_FIELD.setAccessible(true);
        DECORATORS_FIELD.setAccessible(true);
    }

    @ParameterizedTest
    @CsvSource({
        "true, RetrySpanDecorator should be added when retry is enabled",
        "false, RetrySpanDecorator should not be added when retry is disabled"
    })
    void shouldCreatePluginWithRetrySpanDecoratorWhenClientRetryEnabled(
            final boolean isRetryEnabled,
            final String message
    ) throws IllegalAccessException {
        final Plugin plugin = OpenTelemetryPluginFactory.create(createTestClient(isRetryEnabled));

        assertNotNull(plugin);
        assertInstanceOf(OpenTelemetryPlugin.class, plugin);

        final Optional<SpanDecorator> innerCompositeDecorator = StreamSupport.stream(
                ((Iterable<SpanDecorator>) DECORATORS_FIELD.get(SPAN_DECORATOR_FIELD.get(plugin))).spliterator(), false
        ).filter(decorator -> decorator instanceof CompositeSpanDecorator).findFirst();

        assertThat(innerCompositeDecorator).isPresent();
        assertThat(
            StreamSupport.stream(
                ((Iterable<SpanDecorator>) DECORATORS_FIELD.get(innerCompositeDecorator.get())).spliterator(), false
            ).anyMatch(decorator -> decorator instanceof RetrySpanDecorator)
        ).withFailMessage(message).isEqualTo(isRetryEnabled);
    }

    @Test
    void shouldCreatePluginWithoutRetrySpanDecorator() throws IllegalAccessException {
        final Plugin plugin = OpenTelemetryPluginFactory.create(createTestClient(false));

        assertNotNull(plugin);
        assertInstanceOf(OpenTelemetryPlugin.class, plugin);

        final Optional<SpanDecorator> innerCompositeDecorator = StreamSupport.stream(
            ((Iterable<SpanDecorator>) DECORATORS_FIELD.get(SPAN_DECORATOR_FIELD.get(plugin))).spliterator(), false
        ).filter(decorator -> decorator instanceof CompositeSpanDecorator).findFirst();

        assertThat(innerCompositeDecorator).isPresent();
        assertTrue(
            StreamSupport.stream(
                ((Iterable<SpanDecorator>) DECORATORS_FIELD.get(innerCompositeDecorator.get())).spliterator(), false
            ).noneMatch(decorator -> decorator instanceof RetrySpanDecorator),
            "RetrySpanDecorator should not be added when retry is disabled"
        );
    }

    private static RiptideProperties.Client createTestClient(final boolean retryEnabled) {
        final RiptideProperties.Client client = new RiptideProperties.Client();
        final RiptideProperties.Telemetry telemetry = new RiptideProperties.Telemetry();
        telemetry.setAttributes(Map.of("service.name", "test-service"));
        client.setTelemetry(telemetry);

        final RiptideProperties.Retry retry = new RiptideProperties.Retry();
        retry.setEnabled(retryEnabled);
        client.setRetry(retry);

        return client;
    }

}
