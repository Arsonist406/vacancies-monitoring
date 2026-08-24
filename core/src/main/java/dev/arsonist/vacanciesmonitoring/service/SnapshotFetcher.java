package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotFetcher {

    private final Random random;

    public byte[] fetchSnapshot(JobBoard jobBoard) {
        long waitTimeInMillis = random.nextLong(0, 60000);
        try {
            log.info("[ID: {}] - Waiting for {} seconds for randomization of request time",
                    LogContext.getLogId(), Duration.ofMillis(waitTimeInMillis).toSeconds());
            Thread.currentThread().wait(waitTimeInMillis);
        } catch (InterruptedException e) {
            log.error("[ID: {}] - Exception during waiting", LogContext.getLogId(), e);
            throw new RuntimeException(e);
        }

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
