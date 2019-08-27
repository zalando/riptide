package org.zalando.riptide;

import com.google.common.reflect.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;

import java.io.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public interface MessageReader {

    <I> I read(TypeToken<I> type, ClientHttpResponse response) throws IOException;

}
