package org.zalando.riptide.spring;

import lombok.SneakyThrows;
import org.apiguardian.api.API;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
public final class SpringBoot1xSettingsParser implements SettingsParser {

    @Override
    public boolean isApplicable() {
        return ClassUtils.isPresent("org.springframework.boot.bind.PropertiesConfigurationFactory",
                SpringBoot1xSettingsParser.class.getClassLoader());
    }

    @Override
    @SneakyThrows
    public RiptideProperties parse(final ConfigurableEnvironment environment) {
        final PropertiesConfigurationFactory<RiptideProperties> factory =
                new PropertiesConfigurationFactory<>(RiptideProperties.class);

        factory.setTargetName("riptide");
        factory.setPropertySources(environment.getPropertySources());
        factory.setConversionService(environment.getConversionService());

        return factory.getObject();
    }

}
