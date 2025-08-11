package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.ReflectionUtils;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;
import org.zalando.riptide.opentelemetry.span.CompositeSpanDecorator;
import org.zalando.riptide.opentelemetry.span.FlowIdSpanDecorator;
import org.zalando.riptide.opentelemetry.span.RetrySpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenTelemetryPluginFactoryTest {

    private static final Field SPAN_DECORATOR_FIELD = ReflectionUtils.findField(OpenTelemetryPlugin.class, "spanDecorator");
    private static final Field DECORATORS_FIELD = ReflectionUtils.findField(CompositeSpanDecorator.class, "decorators");
    private static final Field PROPAGATE_FLOW_ID_FIELD = ReflectionUtils.findField(FlowIdSpanDecorator.class, "propagateFlowId");

    static {
        SPAN_DECORATOR_FIELD.setAccessible(true);
        DECORATORS_FIELD.setAccessible(true);
        PROPAGATE_FLOW_ID_FIELD.setAccessible(true);
    }

    @ParameterizedTest
    @CsvSource({
        "true, RetrySpanDecorator should be added when retry is enabled",
        "false, RetrySpanDecorator should not be added when retry is disabled"
    })
    void shouldAddRetrySpanDecoratorWhenEnabled(
        final boolean isRetryEnabled,
        final String message
    ) throws IllegalAccessException {
        final Plugin plugin = OpenTelemetryPluginFactory.create(createTestClient(isRetryEnabled, false));

        assertThat(plugin)
            .isNotNull()
            .isInstanceOf(OpenTelemetryPlugin.class);

        final Optional<CompositeSpanDecorator> innerCompositeDecorator = getDecorator(
            (SpanDecorator) SPAN_DECORATOR_FIELD.get(plugin),
            CompositeSpanDecorator.class
        );

        assertThat(innerCompositeDecorator).isPresent();
        assertThat(getDecorator(innerCompositeDecorator.get(), RetrySpanDecorator.class).isPresent())
            .withFailMessage(message)
            .isEqualTo(isRetryEnabled);
    }


    @ParameterizedTest
    @CsvSource({
        "true, FlowIdSpanDecorator should be added when client telemetry is enabled",
        "false, FlowIdSpanDecorator should be added when client telemetry is disabled"
    })
    void shouldAlwaysAddFlowIdSpanDecorator(
        final boolean isFlowIdPropagationEnabled,
        final String message
    ) throws IllegalAccessException {
        final Plugin plugin = OpenTelemetryPluginFactory.create(createTestClient(false, isFlowIdPropagationEnabled));

        assertThat(plugin)
            .isNotNull()
            .isInstanceOf(OpenTelemetryPlugin.class);

        final Optional<CompositeSpanDecorator> innerCompositeDecorator = getDecorator(
            (SpanDecorator) SPAN_DECORATOR_FIELD.get(plugin),
            CompositeSpanDecorator.class
        );

        assertThat(innerCompositeDecorator).isPresent();
        assertThat(getDecorator(innerCompositeDecorator.get(), FlowIdSpanDecorator.class))
            .withFailMessage(message)
            .isPresent();
    }

    @ParameterizedTest
    @CsvSource({
        "true, flow id propagation should be enabled",
        "false, flow id propagation should be enabled be disabled"
    })
    void shouldAddFlowIdHeaderWhenPropagateFlowIdEnabled(
        final boolean isFlowIdPropagationEnabled,
        final String message
    ) throws IllegalAccessException {
        final Plugin plugin = OpenTelemetryPluginFactory.create(createTestClient(false, isFlowIdPropagationEnabled));

        assertThat(plugin)
            .isNotNull()
            .isInstanceOf(OpenTelemetryPlugin.class);

        final Optional<CompositeSpanDecorator> innerCompositeDecorator = getDecorator(
            (SpanDecorator) SPAN_DECORATOR_FIELD.get(plugin),
            CompositeSpanDecorator.class
        );

        assertThat(innerCompositeDecorator).isPresent();

        final Optional<FlowIdSpanDecorator> decoratorCandidate = getDecorator(innerCompositeDecorator.get(), FlowIdSpanDecorator.class);
        assertThat(decoratorCandidate).isNotEmpty();

        final FlowIdSpanDecorator decorator = decoratorCandidate.get();
        assertThat(PROPAGATE_FLOW_ID_FIELD.getBoolean(decorator))
            .withFailMessage(message)
            .isEqualTo(isFlowIdPropagationEnabled);
    }

    private static <T extends SpanDecorator> Optional<T> getDecorator(
        final SpanDecorator decorator,
        final Class<T> decoratorType
    ) throws IllegalAccessException {
        return StreamSupport.stream(((Iterable<SpanDecorator>) DECORATORS_FIELD.get(decorator)).spliterator(), false)
            .filter(decoratorType::isInstance)
            .map(decoratorType::cast)
            .findFirst();
    }

    private static RiptideProperties.Client createTestClient(final boolean retryEnabled,
                                                             final boolean propagateFlowId) {
        final RiptideProperties.Client client = new RiptideProperties.Client();
        final RiptideProperties.Telemetry telemetry = new RiptideProperties.Telemetry();
        telemetry.setAttributes(Map.of("service.name", "test-service"));
        telemetry.setPropagateFlowId(propagateFlowId);
        client.setTelemetry(telemetry);

        final RiptideProperties.Retry retry = new RiptideProperties.Retry();
        retry.setEnabled(retryEnabled);
        client.setRetry(retry);

        return client;
    }

}
