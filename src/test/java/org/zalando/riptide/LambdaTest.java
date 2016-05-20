package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import com.google.common.reflect.TypeToken;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Selectors.status;

/**
 * Tests whether all callbacks/functions can be supplied as lambdas. If this class compiles everything is already fine.
 */
public final class LambdaTest {

    @Test
    public void shouldSupportLambdaOnCaptureResponse() {
        anyStatus().capture(response -> null);
    }

    @Test
    public void shouldSupportLambdaOnCallRunnable() {
        anyStatus().call(() -> {
        });
    }

    @Test
    public void shouldSupportLambdaOnCallResponse() {
        anyStatus().call(response -> {
        });
    }

    @Test
    public void shouldSupportLambdaOnCaptureClassEntity() {
        anyStatus().capture(String.class, (String entity) -> null);
    }

    @Test
    public void shouldSupportLambdaOnCaptureTypeTokenEntity() {
        anyStatus().capture(TypeToken.of(String.class), (String entity) -> null);
    }

    @Test
    public void shouldSupportLambdaOnCaptureClassResponseEntity() {
        anyStatus().capture(String.class, (ResponseEntity<String> entity) -> null);
    }

    @Test
    public void shouldSupportLambdaOnCaptureTypeTokenResponseEntity() {
        anyStatus().capture(TypeToken.of(String.class), (ResponseEntity<String> entity) -> null);
    }

    @Test
    public void shouldSupportLambdaOnCallClassEntity() {
        anyStatus().call(String.class, (String entity) -> {
        });
    }

    @Test
    public void shouldSupportLambdaOnCallTypeTokenEntity() {
        anyStatus().call(TypeToken.of(String.class), (String entity) -> {
        });
    }

    @Test
    public void shouldSupportLambdaOnCallClassResponseEntity() {
        anyStatus().call(String.class, (ResponseEntity<String> entity) -> {
        });
    }

    @Test
    public void shouldSupportLambdaOnCallTypeTokenResponseEntity() {
        anyStatus().call(TypeToken.of(String.class), (ResponseEntity<String> entity) -> {
        });
    }

    @Test
    public void shouldSupportDispatch() {
        anyStatus().dispatch(condition -> condition.capture(String.class));
    }

    @Test
    public void shouldSupportDispatchWithSelector() {
        anyStatus().dispatch(response -> response, status());
    }


}
