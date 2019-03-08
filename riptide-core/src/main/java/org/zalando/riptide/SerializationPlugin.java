package org.zalando.riptide;

import lombok.AllArgsConstructor;

@AllArgsConstructor
final class SerializationPlugin implements Plugin {

    private final MessageWriter writer;

    @Override
    public RequestExecution aroundSerialization(final RequestExecution execution) {
        return arguments -> {
            return execution.execute(arguments
                    .withEntity((message) ->
                            writer.write(message, arguments)));
        };
    }

}
