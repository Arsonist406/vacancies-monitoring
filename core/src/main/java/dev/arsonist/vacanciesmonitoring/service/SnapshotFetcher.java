package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.exception.SnapshotFetchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotFetcher {

    public byte[] fetchSnapshot(JobBoardConfig jobBoardConfig) {
        try {
            return execute(jobBoardConfig);
        } catch (Exception e) {
            log.error("[ID: {}] - Exception during fetching snapshot", LogContext.getLogId(), e);
            throw new SnapshotFetchException(e);
        }
    }

    private byte[] execute(JobBoardConfig jobBoardConfig) {
        byte[] html = RestClient.builder()
                .build()
                .get()
                .uri(jobBoardConfig.url())
                .header(HttpHeaders.ACCEPT_LANGUAGE, "uk;q=0.6")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .retrieve()
                .body(byte[].class);

        if (html == null || html.length == 0) {
            log.error("[ID: {}] - Job board retrieve empty page", LogContext.getLogId());
            throw new SnapshotFetchException("Job board retrieve empty page. Job board config: " + jobBoardConfig);
        }

        return html;
    }
}
