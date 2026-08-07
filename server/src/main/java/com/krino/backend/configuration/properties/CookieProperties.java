package com.krino.backend.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.cookies")
public record CookieProperties(
        @DefaultValue("true") boolean secure
) {}
