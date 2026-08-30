package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.JobBoardCookiesProperties;
import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.exception.SnapshotFetchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotFetcher {

    private final JobBoardCookiesProperties jobBoardCookiesProperties;

    public byte[] fetchSnapshot(JobBoardConfig jobBoardConfig) {
        try {
            return execute(jobBoardConfig);
        } catch (Exception e) {
            log.error("[ID: {}] - Exception during fetching snapshot", LogContext.getLogId(), e);
            throw new SnapshotFetchException(e);
        }
    }

    private byte[] execute(JobBoardConfig jobBoardConfig) {
        Map<String, String> finalHeaders = new HashMap<>(jobBoardConfig.headers());
        String cookies = jobBoardCookiesProperties.get(jobBoardConfig.jobBoard());
        if (StringUtils.hasText(cookies)) {
            finalHeaders.put(HttpHeaders.COOKIE, cookies);
        }

        byte[] html = RestClient.builder()
                .build()
                .get()
                .uri(jobBoardConfig.url())
                .headers(headers -> headers.setAll(finalHeaders))
                .retrieve()
                .body(byte[].class);

        if (html == null || html.length == 0) {
            log.error("[ID: {}] - Job board retrieve empty page", LogContext.getLogId());
            throw new SnapshotFetchException("Job board retrieve empty page. Job board config: " + jobBoardConfig);
        }

        return html;
    }
}
