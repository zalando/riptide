package org.zalando.riptide.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import org.zalando.riptide.RequestArguments;

import java.util.Optional;

import static com.netflix.hystrix.HystrixObservableCommand.Setter.withGroupKey;

public final class DefaultSetterFactory implements SetterFactory {

    @Override
    public HystrixObservableCommand.Setter create(final RequestArguments request) {
        return withGroupKey(createGroupKey(request))
                .andCommandKey(createCommandKey(request));
    }

    public static HystrixCommandGroupKey createGroupKey(final RequestArguments request) {
        return HystrixCommandGroupKey.Factory.asKey(request.getRequestUri().getHost());
    }

    public static HystrixCommandKey createCommandKey(final RequestArguments request) {
        return HystrixCommandKey.Factory.asKey(request.getMethod() + " " +
                Optional.ofNullable(request.getUriTemplate()).orElseGet(request.getRequestUri()::getPath));
    }

}
