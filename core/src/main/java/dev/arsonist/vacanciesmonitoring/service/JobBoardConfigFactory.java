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

    private record JobBoardConfigJson(String url, boolean test) {}

    private final ObjectMapper objectMapper;

    public JobBoardConfig create(JobBoard jobBoard) {
        String path = "job-boards-configs/" + jobBoard.name() + ".json";
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            JobBoardConfigJson json = objectMapper.readValue(is, JobBoardConfigJson.class);
            return new JobBoardConfig(jobBoard, json.url(), json.test());
        } catch (IOException e) {
            throw new RuntimeException("Error during reading config of " + jobBoard, e);
        }
    }
}
