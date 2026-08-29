package dev.arsonist.vacanciesmonitoring.task;

import dev.arsonist.vacanciesmonitoring.config.SnapshotProperties;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.service.JobBoardConfigFactory;
import dev.arsonist.vacanciesmonitoring.service.LogContext;
import dev.arsonist.vacanciesmonitoring.service.MainFlowExecutor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotsFetchTaskFactory {

    private final SnapshotProperties snapshotProperties;
    private final TaskScheduler taskScheduler;
    private final JobBoardConfigFactory jobBoardConfigFactory;
    private final MainFlowExecutor mainFlowExecutor;

    @PostConstruct
    public void init() {
        Arrays.stream(JobBoard.values()).forEach(jobBoard -> {
            var periodicDelay = Duration.ofSeconds(snapshotProperties.fetchDelayInSeconds());
            var trigger = new PeriodicTrigger(periodicDelay);

            var initialDelay = Duration.ofSeconds(20);
            trigger.setInitialDelay(initialDelay);
            trigger.setFixedRate(false);

            var jobBoardConfig = jobBoardConfigFactory.create(jobBoard);
            taskScheduler.schedule(() -> {
                String logId = jobBoardConfig.jobBoard() + ":" + UUID.randomUUID();
                Runnable runnable = () -> mainFlowExecutor.execute(jobBoardConfig);
                LogContext.withLogId(logId, runnable);
            }, trigger);
            log.info("Schedule periodic task for {}", jobBoard);
        });
    }
}
