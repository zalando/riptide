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
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Navigators.status;

/**
 * Tests whether all callbacks/functions can be supplied as lambdas. If this class compiles everything is already fine.
 */
public final class LambdaTest {

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
    public void shouldSupportLambdaOnCallClassEntity() {
        anyStatus().call(String.class, entity -> {
            Assert.assertThat(entity, is(instanceOf(String.class)));
        });
    }

    @Test
    public void shouldSupportLambdaOnCallTypeTokenEntity() {
        anyStatus().call(TypeToken.of(String.class), entity -> {
            Assert.assertThat(entity, is(instanceOf(String.class)));
        });
    }

    @Test
    public void shouldSupportDispatchWithSelector() {
        anyStatus().dispatch(response -> response, status());
    }

}
