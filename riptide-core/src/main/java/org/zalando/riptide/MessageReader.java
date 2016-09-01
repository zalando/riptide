package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public interface MessageReader {

    <I> I read(TypeToken<I> type, ClientHttpResponse response) throws IOException;

}
