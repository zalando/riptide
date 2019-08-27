package org.zalando.riptide.autoconfigure;

import org.hamcrest.*;
import org.mockito.*;

import static org.hamcrest.Matchers.*;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.*;

final class Mocks {

    public static Matcher<Object> isMock() {
        return hasFeature("mock", object -> Mockito.mockingDetails(object).isMock(), is(true));
    }

}
