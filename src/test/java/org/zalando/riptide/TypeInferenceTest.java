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

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zalando.riptide.model.Account;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.zalando.riptide.Capture.listOf;
import static org.zalando.riptide.Capture.mapOf;

public class TypeInferenceTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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

    static Map<List<String>, List<Account>> nestedMapFunction(List<List<Account>> accounts) {
        return null;
    }

    static void nestedMapConsumer(List<Map<List<Account>, List<Map<String, Account>>>> accounts) {
    }

    @Test
    public void shouldInferNestedGenericFunctionMethodReference() {
        final TypeInference.FunctionInfo<List<List<Account>>, Map<List<String>, List<Account>>> info = TypeInference.forFunction(TypeInferenceTest::nestedMapFunction);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(mapOf(listOf(String.class), listOf(Account.class)), info.getReturnType().get());

        Assert.assertEquals(listOf(listOf(Account.class)), info.getArgumentType());
    }

    @Test
    public void shouldInferNestedGenericConsumerMethodReference() {
        final TypeInference.FunctionInfo<List<Map<List<Account>, List<Map<String, Account>>>>, ?> info = TypeInference.forConsumer(TypeInferenceTest::nestedMapConsumer);

        Assert.assertFalse(info.getReturnType().isPresent());

        Assert.assertEquals(listOf(mapOf(listOf(Account.class), listOf(mapOf(String.class, Account.class)))), info.getArgumentType());
    }

    @Test
    public void noGenericSignatureForLocalFunctionLambda() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not determine generic type parameters for raw type");

        ThrowingFunction<List<Account>, List<String>> names = args -> names(args);
        TypeInference.FunctionInfo<List<Account>, List<String>> info = TypeInference.forFunction(names);

        Assert.assertTrue(info.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), info.getReturnType().get());

        Assert.assertEquals(listOf(Account.class), info.getArgumentType());
    }

    @Test
    public void noGenericSignatureForLocalConsumerLambda() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not determine generic type parameters for raw type");

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

    @Test
    public void shouldInferGenericFunctionAndConsumerImplementation() {
        class FunctionAndConsumerAndMore implements ThrowingFunction<List<Account>, List<String>>, ThrowingConsumer<List<Account>>, Comparable<Account> {

            @Override
            public void accept(List<Account> input) throws Exception {

            }

            @Override
            public List<String> apply(List<Account> input) throws Exception {
                return null;
            }

            @Override
            public int compareTo(Account o) {
                return 0;
            }
        }
        final FunctionAndConsumerAndMore accounts = new FunctionAndConsumerAndMore();

        final TypeInference.FunctionInfo<List<Account>, ?> consumerInfo = TypeInference.forConsumer(accounts);

        Assert.assertFalse(consumerInfo.getReturnType().isPresent());
        Assert.assertEquals(listOf(Account.class), consumerInfo.getArgumentType());

        final TypeInference.FunctionInfo<List<Account>, ?> functionInfo = TypeInference.forFunction(accounts);

        Assert.assertTrue(functionInfo.getReturnType().isPresent());
        Assert.assertEquals(listOf(String.class), functionInfo.getReturnType().get());
        Assert.assertEquals(listOf(Account.class), functionInfo.getArgumentType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void noGenericSignatureForDynamicsProxyFunction() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not extract generic types");

        final ThrowingFunction<Account, String> proxy = (ThrowingFunction<Account, String>) Proxy.newProxyInstance(ThrowingFunction.class.getClassLoader(), new Class<?>[]{ThrowingFunction.class}, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(Object o, Method method, Object[] objects) throws Throwable {

                return null;
            }
        });

        TypeInference.forFunction(proxy);
    }

    @Test
    public void noGenericSignatureForBoundedParameter() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not determine generic type parameters for unknown type");

        class BoundedConsumer<T extends Account> implements ThrowingConsumer<T> {

            @Override
            public void accept(T input) throws Exception {

            }
        }

        TypeInference.forConsumer(new BoundedConsumer<>());
    }

    @Test
    public void noGenericSignatureForNestedBoundParameter() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Could not determine generic type parameters for unknown type");

        class BoundedFunction<T extends Account> implements ThrowingFunction<T, List<T>> {

            @Override
            public List<T> apply(T input) throws Exception {
                return null;
            }
        }

        TypeInference.forFunction(new BoundedFunction<>());
    }

}
