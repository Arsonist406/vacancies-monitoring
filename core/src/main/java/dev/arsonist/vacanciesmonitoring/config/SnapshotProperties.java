package dev.arsonist.vacanciesmonitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.snapshots")
public record SnapshotProperties(
        long lifetimeInDays,
        long fetchDelayInSeconds,
        long sleepUpToBeforeFetchInMilliseconds
) {
}
