package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface MessageReader {

    <I> I read(TypeToken<I> type, ClientHttpResponse response) throws IOException;

}
