package org.zalando.riptide;

import org.springframework.http.client.*;

import javax.annotation.*;
import java.io.*;

enum ReasonPhraseNavigator implements EqualityNavigator<String> {

    INSTANCE;

    @Nullable
    @Override
    public String attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getStatusText();
    }

}
