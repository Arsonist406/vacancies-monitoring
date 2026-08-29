package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.SnapshotProperties;
import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.dto.VacancyDto;
import dev.arsonist.vacanciesmonitoring.exception.CoreException;
import dev.arsonist.vacanciesmonitoring.exception.ParserException;
import dev.arsonist.vacanciesmonitoring.exception.TelegramException;
import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import dev.arsonist.vacanciesmonitoring.repository.SnapshotRepository;
import dev.arsonist.vacanciesmonitoring.repository.VacancyRepository;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.CoreErrorMessageBuilder;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.NewVacancyMessageBuilder;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.ParserErrorMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainFlowExecutor {

    private final Random random;
    private final SnapshotFetcher snapshotFetcher;
    private final SnapshotProperties snapshotProperties;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final TelegramNotifier telegramNotifier;
    private final VacancyRepository vacancyRepository;
    private final ParserClient parserClient;

    private final ParserErrorMessageBuilder parserErrorMessageBuilder;
    private final NewVacancyMessageBuilder newVacancyMessageBuilder;
    private final CoreErrorMessageBuilder coreErrorMessageBuilder;

    public void execute(JobBoardConfig jobBoardConfig) {
        log.info("[ID: {}] - Starting main flow execution", LogContext.getLogId());

        String message;
        try {
            message = executeInternal(jobBoardConfig);
        } catch (ParserException e) {
            message = parserErrorMessageBuilder.build(e);
        } catch (Exception e) {
            message = coreErrorMessageBuilder.build(e);
        }

        if (StringUtils.hasText(message)) {
            tryToNotify(message);
        }

        log.info("[ID: {}] - Ending main flow execution", LogContext.getLogId());
    }

    public String executeInternal(JobBoardConfig jobBoardConfig) {
        sleep();

        log.info("[ID: {}] - Fetching new snapshot", LogContext.getLogId());
        byte[] html = snapshotFetcher.fetchSnapshot(jobBoardConfig);

        String snapshotId = UUID.randomUUID().toString();
        var snapshot = Snapshot.builder()
                .id(snapshotId)
                .jobBoard(jobBoardConfig.jobBoard())
                .fetchTime(LocalDateTime.now())
                .html(html)
                .logId(LogContext.getLogId())
                .test(jobBoardConfig.test())
                .build();
        snapshotRepository.save(snapshot);

        log.info("[ID: {}] [Snapshot ID: {}] - Sending parse request",
                LogContext.getLogId(), snapshotId);
        ResponseEntity<String> parseResponse = parserClient.parseRequest(snapshotId);
        return buildMessage(parseResponse, jobBoardConfig);
    }

    private void sleep() {
        long sleepTimeInMillis = random.nextLong(0, snapshotProperties.sleepUpToBeforeFetchInMilliseconds());
        try {
            log.info("[ID: {}] - Sleeping for {} seconds for randomization of request time",
                    LogContext.getLogId(), Duration.ofMillis(sleepTimeInMillis).toSeconds());
            Thread.sleep(sleepTimeInMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[ID: {}] - Exception during sleeping", LogContext.getLogId(), e);
            throw new CoreException(e);
        }
    }

    private String buildMessage(ResponseEntity<String> parseResponse,
                                JobBoardConfig jobBoardConfig) {
        var vacancies = objectMapper.readValue(parseResponse.getBody(), VacancyDto[].class);
        if (jobBoardConfig.test()) {
            if (vacancies.length != 0) {
                log.info("[ID: {}] - Parser is working", LogContext.getLogId());
                return null;
            }

            log.error("[ID: {}] - Parser return 0 vacancies. Probably, dead parser", LogContext.getLogId());
            throw new ParserException("Parser return 0 vacancies");
        } else {
            var filteredVacancies = Arrays.stream(vacancies)
                    .filter(v -> vacancyRepository.existsByKeys(v.companyName(), jobBoardConfig.jobBoard(), v.location(), v.title())
                            .isEmpty())
                    .toList();
            if (filteredVacancies.isEmpty()) {
                log.info("[ID: {}] - No new vacancies", LogContext.getLogId());
                return null;
            }

            var savedVacancies = filteredVacancies.stream()
                    .map(this::mapToVacancy)
                    .map(vacancyRepository::save)
                    .toList();
            return newVacancyMessageBuilder.build(savedVacancies);
        }
    }

    private Vacancy mapToVacancy(VacancyDto vacancyDto) {
        return Vacancy.builder()
                .companyName(vacancyDto.companyName())
                .jobBoard(vacancyDto.jobBoard())
                .location(vacancyDto.location())
                .title(vacancyDto.title())
                .publishTime(vacancyDto.publishTime())
                .url(vacancyDto.url())
                .build();
    }

    private void tryToNotify(String message) {
        boolean userNotified = false;
        for (int i = 1; i < 31 && !userNotified; i++) {
            try {
                log.info("[ID: {}] - Try to notify user. Attempt: {}", LogContext.getLogId(), i);
                telegramNotifier.notify(message);
                userNotified = true;
            } catch (TelegramException exception) {
                // probably, lost internet connection or telegram servers are dead
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[ID: {}] - Exception during sleeping", LogContext.getLogId(), e);
                    throw new CoreException(e);
                }
            }
        }
    }
}
