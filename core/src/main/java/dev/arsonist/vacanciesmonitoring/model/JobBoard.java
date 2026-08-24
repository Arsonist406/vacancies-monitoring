package dev.arsonist.vacanciesmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobBoard {
    DOU("https://jobs.dou.ua/vacancies/?category=Java");

    private final String url;
}
