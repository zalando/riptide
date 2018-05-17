package org.zalando.riptide.spring;

import org.springframework.core.env.ConfigurableEnvironment;

public interface SettingsParser {
    boolean isApplicable();
    RiptideSettings parse(ConfigurableEnvironment environment);
}
