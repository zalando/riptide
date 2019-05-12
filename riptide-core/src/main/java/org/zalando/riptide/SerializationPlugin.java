package org.zalando.riptide;

import lombok.AllArgsConstructor;

@AllArgsConstructor
final class SerializationPlugin implements Plugin {

    private final MessageWriter writer;

    @Override
    public RequestExecution aroundSerialization(final RequestExecution execution) {
        return arguments ->
                execution.execute(arguments.getEntity() == null ?
                        arguments.withEntity(toEntity(arguments)) :
                        arguments);
    }

    private RequestArguments.Entity toEntity(final RequestArguments arguments) {
        return message -> writer.write(message, arguments);
    }

}
