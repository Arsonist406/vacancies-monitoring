package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.exception.CoreException;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobBoardConfigFactory {

    private record JobBoardConfigProjection(
            String url,
            long fetchDelayInSeconds,
            long sleepUpToBeforeFetchInMilliseconds,
            Map<String, String> headers,
            boolean test
    ) {}

    private final ObjectMapper objectMapper;

    public JobBoardConfig create(JobBoard jobBoard) {
        String path = "job-boards-configs/" + jobBoard.name() + ".json";
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            var projection = objectMapper.readValue(is, JobBoardConfigProjection.class);
            return JobBoardConfig.builder()
                    .jobBoard(jobBoard)
                    .url(projection.url())
                    .fetchDelayInSeconds(projection.fetchDelayInSeconds())
                    .sleepUpToBeforeFetchInMilliseconds(projection.sleepUpToBeforeFetchInMilliseconds())
                    .headers(projection.headers())
                    .test(projection.test())
                    .build();
        } catch (IOException e) {
            throw new CoreException("Error during reading config of " + jobBoard, e);
        }
    }
}
