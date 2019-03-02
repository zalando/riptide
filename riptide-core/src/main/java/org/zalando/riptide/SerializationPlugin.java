package org.zalando.riptide;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import java.io.ByteArrayOutputStream;

import static org.springframework.util.CollectionUtils.toMultiValueMap;

@AllArgsConstructor
final class SerializationPlugin implements Plugin {

    private final MessageWriter writer;

    @Override
    public RequestExecution aroundSerialization(final RequestExecution execution) {
        return arguments -> {
            final HttpHeaders headers = new HttpHeaders();
            headers.addAll(toMultiValueMap(arguments.getHeaders()));
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            final HttpOutputMessage output = new CapturingHttpOutputMessage(headers, stream);

            writer.write(output, arguments);

            return execution.execute(arguments
                    .replaceHeaders(headers)
                    .withEntity(stream.toByteArray()));
        };
    }

    @AllArgsConstructor
    @Getter
    private static class CapturingHttpOutputMessage implements HttpOutputMessage {
        private final HttpHeaders headers;
        private final ByteArrayOutputStream body;
    }

}
