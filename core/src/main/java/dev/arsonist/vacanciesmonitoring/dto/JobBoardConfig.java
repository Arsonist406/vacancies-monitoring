package dev.arsonist.vacanciesmonitoring.dto;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import lombok.Builder;

import java.util.Map;

@Builder
public record JobBoardConfig(
        JobBoard jobBoard,
        String filter,
        String url,
        long fetchDelayInSeconds,
        long sleepUpToBeforeFetchInMilliseconds,
        Map<String, String> headers,
        // testing parser correctness. java can have 0 vacancies, but general probably not
        // therefore: general have 0 vacancies -> parser is dead
        boolean test
) {
}