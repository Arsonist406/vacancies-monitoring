package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.ParserProperties;
import dev.arsonist.vacanciesmonitoring.exception.ParserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParserClient {

    private final ParserProperties parserProperties;

    public ResponseEntity<String> parseRequest(String snapshotId) {
        try {
            return execute(snapshotId);
        } catch (Exception e) {
            log.error("[ID: {}] [Snapshot ID: {}] - Exception during parsing request",
                    LogContext.getLogId(), snapshotId, e);
            throw new ParserException(e);
        }
    }

    private ResponseEntity<String> execute(String snapshotId) {
        return RestClient.builder()
                .baseUrl(parserProperties.url())
                .build()
                .get()
                .uri("/parse/" + snapshotId)
                .retrieve()
                .toEntity(String.class);
    }
}
