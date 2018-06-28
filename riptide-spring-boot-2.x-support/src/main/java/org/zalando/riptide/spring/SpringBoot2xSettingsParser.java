package org.zalando.riptide.spring;

import org.apiguardian.api.API;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;

@API(status = INTERNAL)
public final class SpringBoot2xSettingsParser implements SettingsParser {

    @Override
    public boolean isApplicable() {
        return ClassUtils.isPresent("org.springframework.boot.context.properties.bind.Binder",
                SpringBoot2xSettingsParser.class.getClassLoader());
    }

    @Override
    public RiptideProperties parse(final ConfigurableEnvironment environment) {
        final Iterable<ConfigurationPropertySource> sources = from(environment.getPropertySources());
        final Binder binder = new Binder(sources);

        return binder.bind("riptide", RiptideProperties.class).orElseCreate(RiptideProperties.class);
    }

}
