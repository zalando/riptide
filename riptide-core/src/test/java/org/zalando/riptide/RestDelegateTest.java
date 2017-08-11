package org.zalando.riptide;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@Deprecated
@RunWith(Parameterized.class)
public final class RestDelegateTest {

    private final Http delegate = spy(Http.class);
    private final Rest unit = new Rest(delegate);

    private final Method method;

    public RestDelegateTest(final Method method) {
        this.method = method;
    }

    @Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(Http.class.getMethods())
                .filter(method -> !isStatic(method.getModifiers()))
                .map(method -> new Object[] {method})
                .collect(toList());
    }

    @Test
    public void shouldDelegate() throws InvocationTargetException, IllegalAccessException {
        final Object[] parameters = new Object[method.getParameterCount()];

        method.invoke(unit, parameters);
        method.invoke(verify(delegate), parameters);

        verifyNoMoreInteractions(delegate);
    }

}
