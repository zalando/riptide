package org.zalando.riptide.spring;

import org.apiguardian.api.API;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
public interface SettingsParser {
    boolean isApplicable();
    RiptideProperties parse(ConfigurableEnvironment environment);
}
