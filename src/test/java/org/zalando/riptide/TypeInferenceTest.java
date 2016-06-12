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

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.model.Account;

import java.util.List;

import static org.zalando.riptide.Capture.listOf;

public class TypeInferenceTest {

    static List<String> names(List<Account> accounts) {
        return null;
    }

    static void accounts(List<Account> accounts) {

    }

    @Test
    public void shouldInferFunctionMethodReference() {
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(TypeInferenceTest::names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerMethodReference() {
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(TypeInferenceTest::accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }


    @Test
    public void shouldInferFunctionLocalMethodReference() {
        ThrowingFunction<List<Account>, List<String>> names = TypeInferenceTest::names;
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerLocalMethodReference() {
        ThrowingConsumer<List<Account>> accounts = TypeInferenceTest::accounts;
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    @Ignore
    public void shouldInferFunctionLambda() {
        ThrowingFunction<List<Account>, List<String>> names = args -> names(args);
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    @Ignore
    public void shouldInferConsumerLambda() {

        ThrowingConsumer<List<Account>> accounts = (args) -> accounts(args);
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferFunctionImplementation() {
        ThrowingFunction<List<Account>, List<String>> names = new ThrowingFunction<List<Account>, List<String>>() {

            @Override
            public List<String> apply(List<Account> accounts) throws Exception {
                return names(accounts);
            }
        };

        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerImplementation() {
        final ThrowingConsumer<List<Account>> accounts = new ThrowingConsumer<List<Account>>() {
            @Override
            public void accept(List<Account> accounts) throws Exception {

            }
        };

        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }


}
