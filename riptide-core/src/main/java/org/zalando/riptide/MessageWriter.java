package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpOutputMessage;

import java.io.IOException;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
interface MessageWriter {

    void write(HttpOutputMessage request, RequestArguments arguments) throws IOException;

}
