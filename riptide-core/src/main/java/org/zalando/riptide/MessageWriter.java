package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
interface MessageWriter {

    void write(HttpOutputMessage request, RequestArguments arguments) throws IOException;

}
