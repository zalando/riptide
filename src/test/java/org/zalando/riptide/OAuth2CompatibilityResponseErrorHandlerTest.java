package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.zalando.riptide.handler.OAuth2CompatibilityResponseErrorHandler;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

public class OAuth2CompatibilityResponseErrorHandlerTest {

    private final OAuth2CompatibilityResponseErrorHandler unit = new OAuth2CompatibilityResponseErrorHandler();

    @Test
    public void isNoErrorForClientError() throws IOException {
        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.BAD_REQUEST)), is(false));
    }

    @Test
    public void isNoErrorForServerError() throws IOException {
        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.INTERNAL_SERVER_ERROR)),
                is(false));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void throwsResponseWrappedInException() throws IOException {
        final ClientHttpResponse expectedResponse =
                new MockClientHttpResponse(new byte[]{0x13, 0x37}, HttpStatus.INTERNAL_SERVER_ERROR);

        exception.expect(AlreadyConsumedResponseException.class);
        exception.expect(hasFeature("response", AlreadyConsumedResponseException::getResponse, is(expectedResponse)));

        unit.handleError(expectedResponse);
    }

}
