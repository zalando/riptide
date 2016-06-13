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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gag.annotation.remark.Hack;
import org.objectweb.asm.Type;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

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

        Optional<TypeToken<R>> getReturnType() {
            return returnType;
        }

        TypeToken<T> getArgumentType() {
            return argumentType;
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> TypeToken<R> uncheckedTypeToken(java.lang.reflect.Type type) {
        return (TypeToken<R>) TypeToken.of(type);
    }

    private static <R> Optional<TypeToken<R>> returnTypeToken(java.lang.reflect.Type genericReturnType) {
        final TypeToken<Void> voidTypeToken = TypeToken.of(Void.TYPE);
        final TypeToken<R> typeToken = uncheckedTypeToken(genericReturnType);
        return Optional.of(typeToken).filter(type -> !voidTypeToken.equals(type));
    }

    private static <R> Optional<TypeToken<R>> returnTypeToken(java.lang.reflect.Type[] actualTypeParameters) {
        if (actualTypeParameters.length > 1) {
            final TypeToken<R> typeToken = uncheckedTypeToken(actualTypeParameters[1]);
            return Optional.of(typeToken);
        } else {
            return Optional.empty();
        }
    }

    private static void verifyNoRawType(java.lang.reflect.Type type) {
        if (type instanceof Class) {
            final TypeVariable[] typeParameters = ((Class) type).getTypeParameters();
            if (typeParameters.length > 0) {
                throw new IllegalStateException("Could not determine generic type parameters for raw type [" + type.getTypeName() + "]");
            }
        } else if (type instanceof ParameterizedType) {
            final java.lang.reflect.Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            for (java.lang.reflect.Type actualTypeArgument : actualTypeArguments) {
                verifyNoRawType(actualTypeArgument);
            }
        } else {
            throw new IllegalStateException("Could not determine generic type parameters for unknown type [" + type.getTypeName() + "]");
        }
    }

    private static Stream<java.lang.reflect.Type> genericInterfaces(Class<?> clazz) {
        return Arrays.stream(clazz.getGenericInterfaces());
    }

    private static <T, R> FunctionInfo<T, R> getFunctionInfoForClass(Object function, Class<?> functionalInterface) {
        return genericInterfaces(function.getClass())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> type.getRawType() == functionalInterface)
                .map(type -> {
                    final java.lang.reflect.Type[] actualTypeArguments = type.getActualTypeArguments();
                    for (java.lang.reflect.Type actualTypeArgument : actualTypeArguments) {
                        verifyNoRawType(actualTypeArgument);
                    }

                    final Optional<TypeToken<R>> returnType = returnTypeToken(actualTypeArguments);
                    final TypeToken<T> argumentType = uncheckedTypeToken(actualTypeArguments[0]);
                    return new FunctionInfo<>(returnType, argumentType);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not extract generic types from [" + function.getClass() + "]"));
    }

    private static boolean isStatic(final Method method) {
        return (method.getModifiers() & Modifier.STATIC) != 0;
    }

    private static <T, R> FunctionInfo<T, R> getFunctionInfoForMethodReference(Object function) {
        final Method lambdaMethod = getCorrespondingMethod(function);

        java.lang.reflect.Type genericReturnType = lambdaMethod.getGenericReturnType();
        java.lang.reflect.Type genericParameterType = isStatic(lambdaMethod) ? lambdaMethod.getGenericParameterTypes()[0] : lambdaMethod.getDeclaringClass();

        verifyNoRawType(genericReturnType);
        verifyNoRawType(genericParameterType);

        final Optional<TypeToken<R>> returnType = returnTypeToken(genericReturnType);
        final TypeToken<T> argumentType = uncheckedTypeToken(genericParameterType);

        return new FunctionInfo<>(returnType, argumentType);
    }

    @VisibleForTesting
    @Hack("package visible so that we can force the catch clause to be covered")
    static Method getCorrespondingMethod(final Object functionalInterface) {
        try {
            final Method writeReplace = functionalInterface.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            final SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(functionalInterface);

            final String implMethodName = lambda.getImplMethodName();
            final String implMethodSignature = lambda.getImplMethodSignature();

            final Class<?> implClass = loadClass(lambda.getImplClass());

            final Class<?>[] argumentClasses = Arrays.stream(Type.getArgumentTypes(implMethodSignature))
                    .map(Type::getClassName)
                    .map(TypeInference::loadClass)
                    .toArray(Class<?>[]::new);

            return implClass.getDeclaredMethod(implMethodName, argumentClasses);
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

    private static boolean isMethodReference(Object function) {
        // TODO: Make sure this is really the correct way to identify method references and lambdas
        return function.getClass().isSynthetic();
    }

    static <T, R> FunctionInfo<T, R> forFunction(final ThrowingFunction<T, R> function) {

        if (isMethodReference(function)) {
            return getFunctionInfoForMethodReference(function);
        } else {
            return getFunctionInfoForClass(function, ThrowingFunction.class);
        }
    }

    static <T> FunctionInfo<T, Void> forConsumer(final ThrowingConsumer<T> function) {

        if (isMethodReference(function)) {
            return getFunctionInfoForMethodReference(function);
        } else {
            return getFunctionInfoForClass(function, ThrowingConsumer.class);
        }
    }


}
