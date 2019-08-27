package org.zalando.riptide;

import lombok.*;
import org.springframework.http.*;
import org.zalando.riptide.RequestArguments.*;

import java.io.*;

@AllArgsConstructor
final class SerializationPlugin implements Plugin {

    private final MessageWriter writer;

    @Override
    public RequestExecution aroundSerialization(final RequestExecution execution) {
        return arguments -> execution.execute(arguments.getEntity() == null ?
                arguments.withEntity(new SerializingEntity(arguments)) :
                arguments);
    }

    @AllArgsConstructor
    private class SerializingEntity implements Entity {

        private final RequestArguments arguments;

        @Override
        public void writeTo(final HttpOutputMessage message) throws IOException {
            writer.write(message, arguments);
        }

        @Override
        public boolean isEmpty() {
            return arguments.getBody() == null;
        }

    }

}
