package dev.arsonist.vacanciesmonitoring.dto;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;

public record VacancyDto(
        JobBoard jobBoard,
        String companyName,
        String location,
        String title,
        String publishTime,
        String url
) {
}
