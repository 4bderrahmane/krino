package com.krino.backend.configuration.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CookiePropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void secureDefaultsToTrue() {
        contextRunner.run(context ->
                assertThat(context.getBean(CookieProperties.class).secure()).isTrue());
    }

    @Test
    void secureCanBeDisabledExplicitly() {
        contextRunner
                .withPropertyValues("app.cookies.secure=false")
                .run(context ->
                        assertThat(context.getBean(CookieProperties.class).secure()).isFalse());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CookieProperties.class)
    static class TestConfiguration {}
}
