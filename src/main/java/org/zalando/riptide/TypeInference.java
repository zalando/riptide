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
import org.objectweb.asm.Type;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Optional;

class TypeInference {
    TypeInference() {

    }

    static class FunctionInfo<T, R> {
        private final Optional<TypeToken<R>> returnType;
        private final TypeToken<T> argumentType;

        FunctionInfo(Optional<TypeToken<R>> returnType, TypeToken<T> argumentType) {
            this.returnType = returnType;
            this.argumentType = argumentType;
        }

        public Optional<TypeToken<R>> getReturnType() {
            return returnType;
        }

        TypeToken<T> getArgumentType() {
            return argumentType;
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> Optional<TypeToken<R>> returnTypeToken(Method lambdaMethod) {
        final TypeToken<Void> voidTypeToken = TypeToken.of(Void.TYPE);
        return Optional.of((TypeToken<R>)TypeToken.of(lambdaMethod.getGenericReturnType())).filter(type -> !voidTypeToken.equals(type));
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeToken<T> argumentTypeToken(Method lambdaMethod) {
        return (TypeToken<T>) TypeToken.of(lambdaMethod.getGenericParameterTypes()[0]);
    }

    @SuppressWarnings("unchecked")
    private static <R> Optional<TypeToken<R>> returnTypeToken(java.lang.reflect.Type[] actualTypeParameters) {
        if (actualTypeParameters.length > 1) {
            return Optional.of((TypeToken<R>) TypeToken.of(actualTypeParameters[1]));
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> TypeToken<R> argumentTypeToken(java.lang.reflect.Type[] actualTypeParameters) {
        return (TypeToken<R>) TypeToken.of(actualTypeParameters[0]);
    }

    private static <T, R> FunctionInfo<T, R> getFunctionInfoForClass(Object function, Class<?> functionalInterface) {
        final java.lang.reflect.Type[] genericInterfaces = function.getClass().getGenericInterfaces();

        return Arrays.stream(genericInterfaces)
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> type.getRawType() == functionalInterface)
                .map(type -> {
                    final java.lang.reflect.Type[] actualTypeArguments = type.getActualTypeArguments();
                    final Optional<TypeToken<R>> returnType = returnTypeToken(actualTypeArguments);
                    final TypeToken<T> argumentType = argumentTypeToken(actualTypeArguments);
                    return new FunctionInfo<>(returnType, argumentType);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not extract generic types from [" + function.getClass() + "]"));
    }

    private static <T, R> FunctionInfo<T, R> getFunctionInfoForMethodReference(Object function) {
        final Method lambdaMethod = getCorrespondingMethod(function);

        final Optional<TypeToken<R>> returnType = returnTypeToken(lambdaMethod);
        final TypeToken<T> argumentType = argumentTypeToken(lambdaMethod);

        return new FunctionInfo<>(returnType, argumentType);
    }

    private static Method getCorrespondingMethod(Object functionalInterface) {
        try {
            Method writeReplace = functionalInterface.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(functionalInterface);
            Class<?> implClass = loadClass(lambda.getImplClass());
            Type[] argumentTypes = Type.getArgumentTypes(lambda.getImplMethodSignature());
            Class<?>[] argumentClasses = Arrays.stream(argumentTypes).map(Type::getClassName).map(TypeInference::loadClass).toArray(Class<?>[]::new);

            Method lambdaMethod = implClass.getDeclaredMethod(lambda.getImplMethodName(), argumentClasses);
            boolean synthetic = lambdaMethod.isSynthetic();

            if (synthetic) {
                throw new IllegalStateException("Extracting generic types from lambda functions is not supported, please use method references or explicit type parameters");
            }

            return lambdaMethod;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<?> loadClass(final String className) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            return cl.loadClass(className.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class [" + className + "]", e);
        }
    }

    static <T, R> FunctionInfo<T, R> forFunction(final ThrowingFunction<T, R> function) {

        if (function.getClass().isSynthetic()) {
            return getFunctionInfoForMethodReference(function);
        } else {
            return getFunctionInfoForClass(function, ThrowingFunction.class);
        }
    }

    static <T> FunctionInfo<T, Void> forConsumer(final ThrowingConsumer<T> function) {

        if (function.getClass().isSynthetic()) {
            return getFunctionInfoForMethodReference(function);
        } else {
            return getFunctionInfoForClass(function, ThrowingConsumer.class);
        }
    }


}
