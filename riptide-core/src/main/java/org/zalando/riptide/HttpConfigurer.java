package org.zalando.riptide;

@FunctionalInterface
public interface HttpConfigurer {

    void configure(final HttpBuilder builder);

}
