package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.ParserProperties;
import dev.arsonist.vacanciesmonitoring.config.SnapshotProperties;
import dev.arsonist.vacanciesmonitoring.dto.FastApiError;
import dev.arsonist.vacanciesmonitoring.dto.VacancyDto;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import dev.arsonist.vacanciesmonitoring.repository.SnapshotRepository;
import dev.arsonist.vacanciesmonitoring.repository.VacancyRepository;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.DeadParserMessageBuilder;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.ErrorMessageBuilder;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.NewVacancyMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
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
    private final ParserProperties parserProperties;
    private final SnapshotProperties snapshotProperties;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final TelegramNotifier telegramNotifier;
    private final VacancyRepository vacancyRepository;

    private final ErrorMessageBuilder errorMessageBuilder;
    private final NewVacancyMessageBuilder newVacancyMessageBuilder;
    private final DeadParserMessageBuilder deadParserMessageBuilder;

    public void execute(JobBoard jobBoard) {
        log.info("[ID: {}] - Starting main flow execution", LogContext.getLogId());

        sleep();
        String snapshotId = UUID.randomUUID().toString();
        byte[] gzippedHtml = snapshotFetcher.fetchSnapshot(jobBoard);
        var snapshot = Snapshot.builder()
                .id(snapshotId)
                .jobBoard(jobBoard)
                .fetchTime(LocalDateTime.now())
                .gzippedHtml(gzippedHtml)
                .logId(LogContext.getLogId())
                .build();
        snapshotRepository.save(snapshot);

        String message = buildMessage(snapshotId, jobBoard);
        if (StringUtils.hasText(message)) {
            log.info("[ID: {}] - Sending message to telegram", LogContext.getLogId());
            telegramNotifier.notify(message);
        }

        log.info("[ID: {}] - Ending main flow execution", LogContext.getLogId());
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
            throw new RuntimeException(e);
        }
    }

    private String buildMessage(String snapshotId,
                                JobBoard jobBoard) {
        ResponseEntity<String> parseResponse = sendParseRequest(snapshotId);
        if (!parseResponse.getStatusCode().is2xxSuccessful()) {
            var error = objectMapper.readValue(parseResponse.getBody(), FastApiError.class);
            return errorMessageBuilder.build(error);

        } else {
            var vacancies = objectMapper.readValue(parseResponse.getBody(), VacancyDto[].class);
            if (jobBoard.isTest()) {
                if (vacancies.length != 0) {
                    log.info("[ID: {}] - Parser is working", LogContext.getLogId());
                    return null;
                }

                log.error("[ID: {}] - Parser return 0 vacancies. Probably, dead parser", LogContext.getLogId());
                var updatedSnapshot = snapshotRepository.findById(snapshotId).get();
                return deadParserMessageBuilder.build(updatedSnapshot);
            } else {
                var filteredVacancies = Arrays.stream(vacancies)
                        .filter(v -> vacancyRepository.existsByKeys(v.companyName(), jobBoard, v.location(), v.title())
                                .isEmpty())
                        .toArray(VacancyDto[]::new);
                if (filteredVacancies.length == 0) {
                    log.info("[ID: {}] - No new vacancies", LogContext.getLogId());
                    return null;
                }

                Arrays.stream(filteredVacancies)
                        .map(this::mapToVacancy)
                        .forEach(vacancyRepository::save);
                return newVacancyMessageBuilder.build(vacancies);
            }
        }
    }

    private ResponseEntity<String> sendParseRequest(String snapshotId) {
        return RestClient.builder()
                .baseUrl(parserProperties.url())
                .build()
                .get()
                .uri("/parse/" + snapshotId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, res) -> {
                    // errors are ok here - message should be forwarded to telegram
                    log.error("[ID: {}] - Error during parsing snapshot {}", LogContext.getLogId(), snapshotId);
                })
                .toEntity(String.class);
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
}
