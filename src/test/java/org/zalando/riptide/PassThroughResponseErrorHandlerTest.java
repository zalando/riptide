package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.zalando.riptide.PassThroughResponseErrorHandler;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PassThroughResponseErrorHandlerTest {

    private final PassThroughResponseErrorHandler unit = new PassThroughResponseErrorHandler();

    @Test
    public void isNoErrorForClientError() throws IOException {
        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.BAD_REQUEST)), is(false));
    }

    @Test
    public void isNoErrorForServerError() throws IOException {


        assertThat(unit.hasError(new MockClientHttpResponse(new byte[]{}, HttpStatus.INTERNAL_SERVER_ERROR)),
                is(false));
    }

    @Test
    public void doesNothingWithResponseOnHandleError() throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);

        unit.handleError(response);

        verifyNoMoreInteractions(response);
    }

}
