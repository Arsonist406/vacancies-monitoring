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

    @Scheduled(fixedRate = 3, timeUnit = TimeUnit.HOURS)
    public void cleanup() {
        log.info("Starting cleanup task");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(snapshotProperties.lifetimeInDays());
        snapshotRepository.deleteOldSnapshots(cutoff);
        log.info("Ending cleanup task. Deleted snapshots fetched before {}", cutoff);
    }
}
