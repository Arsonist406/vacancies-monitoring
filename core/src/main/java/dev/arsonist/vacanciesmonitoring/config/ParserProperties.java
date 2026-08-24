package dev.arsonist.vacanciesmonitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.parser")
public record ParserProperties(
        String url
) {
}
