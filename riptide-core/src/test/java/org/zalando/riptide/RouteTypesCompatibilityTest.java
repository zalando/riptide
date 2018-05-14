package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Deprecated
public final class RouteTypesCompatibilityTest {
    
    @Test
    public void listOf() {
        assertEquals(Types.listOf(String.class), Route.listOf(String.class));
    }

    @Test
    public void listOfType() {
        assertEquals(Types.listOf(TypeToken.of(String.class)), Route.listOf(TypeToken.of(String.class)));
    }

    @Test
    public void responseEntityOf() {
        assertEquals(Types.responseEntityOf(String.class), Route.responseEntityOf(String.class));
    }

    @Test
    public void responseEntityOfType() {
        assertEquals(Types.responseEntityOf(TypeToken.of(String.class)), Route.responseEntityOf(TypeToken.of(String.class)));
    }

}
