package org.zalando.riptide.spring;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;

public final class SpringBoot2xSettingsParser implements SettingsParser {

    @Override
    public boolean isApplicable() {
        return ClassUtils.isPresent("org.springframework.boot.context.properties.bind.Binder",
                SpringBoot2xSettingsParser.class.getClassLoader());
    }

    @Override
    public RiptideSettings parse(final ConfigurableEnvironment environment) {
        final Iterable<ConfigurationPropertySource> sources = from(environment.getPropertySources());
        final Binder binder = new Binder(sources);

        return binder.bind("riptide", RiptideSettings.class).orElseCreate(RiptideSettings.class);
    }

}
