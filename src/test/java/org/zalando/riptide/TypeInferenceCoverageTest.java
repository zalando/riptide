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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public class TypeInferenceCoverageTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final String FUNCTION_SIGNATURE = "(Ljava/lang/Object)Ljava/lang/Object";

    @Test
    public void shouldVisitCatchClassNotFoundException() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, NoSuchFieldException {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not load class");

        TypeInference.getCorrespondingMethod(new Function<Object, Object>() {
            Object writeReplace() {
                return new SerializedLambda(
                        TypeInferenceCoverageTest.class,
                        ThrowingFunction.class.getName().replace('.', '/'),
                        "apply",
                        FUNCTION_SIGNATURE,
                        6,
                        "no/such/package/NoSuchClass",
                        "apply",
                        FUNCTION_SIGNATURE,
                        FUNCTION_SIGNATURE,
                        new Object[0]);
            }

            @Override
            public Object apply(Object input) {
                return null;
            }
        });
    }

    @Test
    public void shouldVisitCatchNoSuchMethodException() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectCause(Matchers.instanceOf(NoSuchMethodException.class));

        TypeInference.getCorrespondingMethod(new Function<Object, Object>() {
            @Override
            public Object apply(Object input) {
                return null;
            }
        });
    }

}
