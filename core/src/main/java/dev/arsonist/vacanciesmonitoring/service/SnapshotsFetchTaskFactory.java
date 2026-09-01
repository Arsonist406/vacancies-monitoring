package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
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

    private final TaskScheduler taskScheduler;
    private final JobBoardConfigFactory jobBoardConfigFactory;
    private final MainFlowExecutor mainFlowExecutor;

    @PostConstruct
    public void init() {
        Arrays.stream(JobBoard.values()).forEach(jobBoard -> {
            jobBoardConfigFactory.create(jobBoard).forEach(jobBoardConfig -> {
                var periodicDelay = Duration.ofSeconds(jobBoardConfig.fetchDelayInSeconds());
                var trigger = new PeriodicTrigger(periodicDelay);

                var initialDelay = Duration.ofSeconds(20);
                trigger.setInitialDelay(initialDelay);
                trigger.setFixedRate(false);

                taskScheduler.schedule(() -> {
                    String logId = jobBoardConfig.filter() + ":" + UUID.randomUUID();
                    Runnable runnable = () -> mainFlowExecutor.execute(jobBoardConfig);
                    LogContext.withLogId(logId, runnable);
                }, trigger);
                log.info("Schedule periodic task for {}", jobBoard);
            });
        });
    }
}
