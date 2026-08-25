package dev.arsonist.vacanciesmonitoring.dto;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;

public record JobBoardConfig(
        JobBoard jobBoard,
        String url,
        // testing parser correctness. java can have 0 vacancies, but general (like marketing) probably not
        // therefore: general have 0 vacancies -> parser dead
        boolean test
) {
}