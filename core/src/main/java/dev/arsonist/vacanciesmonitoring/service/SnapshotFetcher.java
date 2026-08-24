package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotFetcher {

    public byte[] fetchSnapshot(JobBoard jobBoard) {
        log.info("[ID: {}] - Fetching new snapshot", LogContext.getLogId());
        byte[] html = RestClient.builder()
                .build()
                .get()
                .uri(jobBoard.getUrl())
                .header(HttpHeaders.ACCEPT_LANGUAGE, "uk;q=0.6")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .retrieve()
                .body(byte[].class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            // todo: add handling when html == null
            gzip.write(html);
        } catch (Exception e) {
            log.error("[ID: {}] - Exception during page encoding", LogContext.getLogId(), e);
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
