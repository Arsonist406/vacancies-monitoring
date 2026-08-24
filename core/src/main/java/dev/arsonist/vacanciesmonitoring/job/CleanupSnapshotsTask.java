package dev.arsonist.vacanciesmonitoring.job;

import dev.arsonist.vacanciesmonitoring.config.SnapshotProperties;
import dev.arsonist.vacanciesmonitoring.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupSnapshotsTask {

    private final SnapshotProperties snapshotProperties;
    private final SnapshotRepository snapshotRepository;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanup() {
        log.info("Starting cleanup task");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(snapshotProperties.lifetimeInDays());
        snapshotRepository.deleteOldSnapshots(false, cutoff);
        log.info("Deleted snapshots fetched before {}", cutoff);

        LocalDateTime testCutoff = LocalDateTime.now().minusDays(snapshotProperties.testLifetimeInDays());
        snapshotRepository.deleteOldSnapshots(true, testCutoff);
        log.info("Deleted test snapshots fetched before {}", testCutoff);

        log.info("Ending cleanup task");
    }
}
