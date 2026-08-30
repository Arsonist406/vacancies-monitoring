package dev.arsonist.vacanciesmonitoring.config;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.job-board-cookie")
public record JobBoardCookiesProperties(
        String djinni
) {
    public String get(JobBoard jobBoard) {
        return switch (jobBoard) {
            case DJINNI_GENERAL, DJINNI_JAVA -> djinni;
            default -> null;
        };
    }
}
