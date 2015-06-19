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

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

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
