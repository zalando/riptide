package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;

enum ReasonPhraseNavigator implements EqualityNavigator<String> {

    INSTANCE;

    @Nullable
    @Override
    public String attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getStatusText();
    }

}
