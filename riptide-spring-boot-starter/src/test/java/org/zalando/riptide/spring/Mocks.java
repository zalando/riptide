package org.zalando.riptide.spring;

import org.hamcrest.Matcher;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;

final class Mocks {

    public static Matcher<Object> isMock() {
        return hasFeature("mock", object -> Mockito.mockingDetails(object).isMock(), is(true));
    }

}
