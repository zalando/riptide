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
import org.junit.*;
import org.zalando.riptide.model.Account;

import java.util.List;

import static org.zalando.riptide.Capture.listOf;

public class TypeInferenceTest {

    static void account(Account account) {

    }

    static List<String> names(List<Account> accounts) {
        return null;
    }

    static void accounts(List<Account> accounts) {

    }


    @Test
    public void shouldInferFunctionMethodReference() {
        TypeInference.FunctionInfo<Account, String> info = TypeInference.forFunction(Account::getName);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(String.class), info.getReturnType().get());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerMethodReference() {
        TypeInference.FunctionInfo<Account, ?> info = TypeInference.forConsumer(TypeInferenceTest::account);

        Assert.assertFalse(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferFunctionLocalMethodReference() {
        ThrowingFunction<Account, String> name = Account::getName;
        TypeInference.FunctionInfo<Account, String> info = TypeInference.forFunction(name);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(String.class), info.getReturnType().get());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerLocalMethodReference() {
        ThrowingConsumer<Account> account = TypeInferenceTest::account;
        TypeInference.FunctionInfo<Account, ?> info = TypeInference.forConsumer(account);

        Assert.assertFalse(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferFunctionLambda() {
        ThrowingFunction<Account, String> name = args -> args.getName();
        TypeInference.FunctionInfo<Account, String> info = TypeInference.forFunction(name);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(String.class), info.getReturnType().get());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerLambda() {

        ThrowingConsumer<Account> account = (args) -> {};
        TypeInference.FunctionInfo<Account, ?> info = TypeInference.forConsumer(account);

        Assert.assertFalse(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferFunctionImplementation() {
        ThrowingFunction<Account, String> name = new ThrowingFunction<Account, String>() {

            @Override
            public String apply(Account input) throws Exception {
                return null;
            }
        };

        TypeInference.FunctionInfo<Account, String> info = TypeInference.forFunction(name);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(String.class), info.getReturnType().get());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferConsumerImplementation() {
        final ThrowingConsumer<Account> account = new ThrowingConsumer<Account>() {
            @Override
            public void accept(Account input) throws Exception {

            }
        };

        TypeInference.FunctionInfo<Account, ?> info = TypeInference.forConsumer(account);

        Assert.assertFalse(info.getReturnType().isPresent());
        Assert.assertEquals(TypeToken.of(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferGenericFunctionMethodReference() {
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(TypeInferenceTest::names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferGenericConsumerMethodReference() {
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(TypeInferenceTest::accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }


    @Test
    public void shouldInferGenericFunctionLocalMethodReference() {
        ThrowingFunction<List<Account>, List<String>> names = TypeInferenceTest::names;
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferGenericConsumerLocalMethodReference() {
        ThrowingConsumer<List<Account>> accounts = TypeInferenceTest::accounts;
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    @Ignore("OpenJDK does not emit the generic signature for generated synthetic methods")
    public void shouldInferGenericFunctionLambda() {
        ThrowingFunction<List<Account>, List<String>> names = args -> names(args);
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    @Ignore("OpenJDK does not emit the generic signature for generated synthetic methods")
    public void shouldInferGenericConsumerLambda() {

        ThrowingConsumer<List<Account>> accounts = (args) -> accounts(args);
        TypeInference.FunctionInfo<List<Account>, ?> info = TypeInference.forConsumer(accounts);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void shouldInferGenericFunctionImplementation() {
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
    public void shouldInferGenericConsumerImplementation() {
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
