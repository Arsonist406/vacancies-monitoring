package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.exception.EmptyHtmlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotFetcher {

    public byte[] fetchSnapshot(JobBoardConfig jobBoardConfig) {
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
            throw new EmptyHtmlException("Job board retrieve empty page. Job board config: " + jobBoardConfig);
        }

        return html;
    }
}
