package org.zalando.riptide;

@FunctionalInterface
public interface RestConfigurer {

    void configure(final HttpBuilder builder);

}
