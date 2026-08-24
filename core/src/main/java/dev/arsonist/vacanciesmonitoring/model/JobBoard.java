package dev.arsonist.vacanciesmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobBoard {
    DOU_JAVA("https://jobs.dou.ua/vacancies/?category=Java", false),
    DOU_MARKETING("https://jobs.dou.ua/vacancies/?category=Marketing", true);

    private final String url;
    // testing parser correctness. java can have 0 vacancies, but general (like marketing) probably not
    // therefore: general have 0 vacancies -> parser dead
    private final boolean test;
}
