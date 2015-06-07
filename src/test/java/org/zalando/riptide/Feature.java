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

import com.google.common.base.Strings;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import static org.hamcrest.Matchers.allOf;

// TODO move this to it's own library
public final class Feature {

    @SafeVarargs
    public static <T, U> Matcher<T> feature(Function<T, U> accessor, Matcher<U>... matchers) {
        final String name = name(accessor);
        final String description = leftTrim(name, "is", "get");
        final String featureName = name + "()";
        return new FeatureMatcher<T, U>(allOf(matchers), description, featureName) {
            @Override
            protected U featureValueOf(T actual) {
                return accessor.apply(actual);
            }
        };
    }

    private static String leftTrim(String name, String... prefixes) {
        for (String prefix : prefixes) {
            if (prefix.equals(Strings.commonPrefix(name, prefix))) {
                // TODO use something else than regex
                return name.replaceFirst(prefix, "");
            }
        }

        return name;
    }

    private static <L> String name(L lambda) {
        for (Class<?> type = lambda.getClass(); type != null; type = type.getSuperclass()) {
            try {
                final Method method = type.getDeclaredMethod("writeReplace");
                method.setAccessible(true);
                final Object replacement = method.invoke(lambda);

                if (replacement instanceof SerializedLambda) {
                    final SerializedLambda serializedLambda = (SerializedLambda) replacement;
                    return serializedLambda.getImplMethodName();
                } else {
                    break;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                break;
            }
        }

        throw new IllegalArgumentException("Expected a method reference, got " + lambda);
    }

}
