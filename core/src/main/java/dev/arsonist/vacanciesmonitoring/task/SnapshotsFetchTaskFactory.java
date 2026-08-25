package dev.arsonist.vacanciesmonitoring.task;

import dev.arsonist.vacanciesmonitoring.config.SnapshotProperties;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.service.JobBoardConfigFactory;
import dev.arsonist.vacanciesmonitoring.service.LogContext;
import dev.arsonist.vacanciesmonitoring.service.MainFlowExecutor;
import dev.arsonist.vacanciesmonitoring.service.TelegramNotifier;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.CoreErrorMessageBuilder;
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
    private final MainFlowExecutor mainFlowExecutor;
    private final JobBoardConfigFactory jobBoardConfigFactory;
    private final TelegramNotifier telegramNotifier;
    private final CoreErrorMessageBuilder coreErrorMessageBuilder;

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
                String logId = jobBoard + ":" + UUID.randomUUID();
                Runnable runnable = () -> {
                    try {
                        mainFlowExecutor.execute(jobBoardConfig);
                    } catch (Exception exception) {
                        tryToNotify(exception);
                        throw new RuntimeException(exception);
                    }
                };
                LogContext.withLogId(logId, runnable);
            }, trigger);
            log.info("Schedule periodic task for {}", jobBoard);
        });
    }

    private void tryToNotify(Exception exception) {
        String message = coreErrorMessageBuilder.build(exception);

        boolean userNotified = false;
        for (int i = 1; i < 31 && !userNotified; i++) {
            try {
                log.info("[ID: {}] - Try to notify user. Attempt: {}", LogContext.getLogId(), i);
                telegramNotifier.notify(message);
                userNotified = true;
            } catch (Exception notificationException) {
                // probably, lost internet connection or telegram servers are dead
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[ID: {}] - Exception during sleeping", LogContext.getLogId(), e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
