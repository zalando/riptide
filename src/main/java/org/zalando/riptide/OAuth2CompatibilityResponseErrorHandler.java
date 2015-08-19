package org.zalando.riptide;

/*
 * ⁣​
 * riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.zalando.riptide.BufferingClientHttpResponse.buffer;

/**
 * Special {@link ResponseErrorHandler} to be used with the <i>OAuth2RestTemplate</i>
 * 
 * Note: When using Springs OAuth2 RestTemplate an <i>OAuth2ErrorHandler</i> will be registered on the
 * {@link RestTemplate}, which wraps the actual one. This error handler may call the actual error handler
 * although it does not deem the response as an error. Snippet below shows that <i>OAuth2ErrorHandler</i>
 * deems all 4xx response codes as an error and will call the actual handler regardless of its behavior.
 * <pre><code>
 *     public boolean hasError(ClientHttpResponse response) throws IOException {
 *           return HttpStatus.Series.CLIENT_ERROR.equals(response.getStatusCode().series())
 *             || this.errorHandler.hasError(response);
 *           }
 * </code></pre>
 * As the <i>OAuth2ErrorHandler</i> will have the response already consumed at this point, it passes a
 * buffered response to the actual error handler. Therefore the only chance to process the response is to
 * process the buffered response of the actual error handler.
 * 
 * To do so this error handler propagates the buffered response back to the {@link Dispatcher} for dispatching
 * by throwing an exception containing the response. {@link Rest} catches this exception and continues with
 * the normal execution path.
 */
public final class OAuth2CompatibilityResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(final ClientHttpResponse response) {
        return false;
    }

    @Override
    public void handleError(final ClientHttpResponse response) throws IOException {
        throw new AlreadyConsumedResponseException(buffer(response));
    }

}
