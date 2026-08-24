package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.ParserProperties;
import dev.arsonist.vacanciesmonitoring.dto.FastApiError;
import dev.arsonist.vacanciesmonitoring.dto.VacancyDto;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import dev.arsonist.vacanciesmonitoring.repository.SnapshotRepository;
import dev.arsonist.vacanciesmonitoring.repository.VacancyRepository;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.ErrorMessageBuilder;
import dev.arsonist.vacanciesmonitoring.service.messagebuilder.NewVacancyMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainFlowExecutor{

    private static final ZoneId KYIV_ZONE_ID = ZoneId.of("Europe/Kyiv");

    private final SnapshotFetcher snapshotFetcher;
    private final ParserProperties parserProperties;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final TelegramNotifier telegramNotifier;
    private final ErrorMessageBuilder errorMessageBuilder;
    private final NewVacancyMessageBuilder newVacancyMessageBuilder;
    private final VacancyRepository vacancyRepository;

    public void execute(JobBoard jobBoard) {
        log.info("[ID: {}] - Starting main flow execution", LogContext.getLogId());

        String snapshotId = UUID.randomUUID().toString();
        byte[] gzippedHtml = snapshotFetcher.fetchSnapshot(jobBoard);
        var snapshot = Snapshot.builder()
                .id(snapshotId)
                .jobBoard(jobBoard)
                .fetchTime(LocalDateTime.now())
                .gzippedHtml(gzippedHtml)
                .build();
        snapshotRepository.save(snapshot);

        String message;
        ResponseEntity<String> parseResponse = sendParseRequest(snapshotId);
        if (!parseResponse.getStatusCode().is2xxSuccessful()) {
            var error = objectMapper.readValue(parseResponse.getBody(), FastApiError.class);
            message = errorMessageBuilder.build(error);
        } else {
            var vacancies = objectMapper.readValue(parseResponse.getBody(), VacancyDto[].class);
            var filteredVacancies = Arrays.stream(vacancies)
                    .filter(v -> !vacancyRepository.existsByKeys(v.companyName(), jobBoard, v.location(), v.title()))
                    .toArray(VacancyDto[]::new);
            if (filteredVacancies.length == 0) {
                log.info("[ID: {}] - No new vacancies. Ending main flow execution", LogContext.getLogId());
                return;
            }

            Arrays.stream(filteredVacancies)
                    .map(this::mapToVacancy)
                    .forEach(vacancyRepository::save);
            message = newVacancyMessageBuilder.build(vacancies);
        }

        log.info("[ID: {}] - Sending message to telegram", LogContext.getLogId());
        telegramNotifier.notify(message);

        log.info("[ID: {}] - Ending main flow execution", LogContext.getLogId());
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
        var publishTime = OffsetDateTime.parse(vacancyDto.publishTime())
                .atZoneSameInstant(KYIV_ZONE_ID)
                .toLocalDateTime();

        return Vacancy.builder()
                .companyName(vacancyDto.companyName())
                .jobBoard(vacancyDto.jobBoard())
                .location(vacancyDto.location())
                .title(vacancyDto.title())
                .publishTime(publishTime)
                .url(vacancyDto.url())
                .build();
    }
}
