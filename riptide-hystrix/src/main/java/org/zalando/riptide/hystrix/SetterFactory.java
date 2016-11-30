package org.zalando.riptide.hystrix;

import com.netflix.hystrix.HystrixObservableCommand;
import org.zalando.riptide.RequestArguments;

@FunctionalInterface
public interface SetterFactory {

    HystrixObservableCommand.Setter create(final RequestArguments arguments);

}
