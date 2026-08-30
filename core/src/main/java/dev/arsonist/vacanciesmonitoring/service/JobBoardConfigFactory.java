package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.dto.JobBoardConfig;
import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class JobBoardConfigFactory {

    private record JobBoardConfigProjection(
            String url,
            long fetchDelayInSeconds,
            boolean test
    ) {}

    private final ObjectMapper objectMapper;

    public JobBoardConfig create(JobBoard jobBoard) {
        String path = "job-boards-configs/" + jobBoard.name() + ".json";
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            JobBoardConfigProjection json = objectMapper.readValue(is, JobBoardConfigProjection.class);
            return new JobBoardConfig(jobBoard, json.url(), json.fetchDelayInSeconds(), json.test());
        } catch (IOException e) {
            throw new RuntimeException("Error during reading config of " + jobBoard, e);
        }
    }
}
