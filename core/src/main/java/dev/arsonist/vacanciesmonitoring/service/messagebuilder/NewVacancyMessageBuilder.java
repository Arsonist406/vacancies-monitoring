package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.dto.VacancyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NewVacancyMessageBuilder {

    public String build(VacancyDto[] vacancies) {
        String body = Arrays.stream(vacancies)
                .map(this::formatVacancy)
                .collect(Collectors.joining("\n\n"));

        return "<b>🫪🔥New Vacancy🔥🫪</b>\n\n" + body;
    }

    private String formatVacancy(VacancyDto vacancyDto) {
        return """
                <b>Title:</b> %s
                <b>Company:</b> %s
                <b>Location:</b> %s
                <b>Published:</b> %s
                <b>URL:</b> <a href="%s">Link</a>"""
                .formatted(
                        escape(vacancyDto.title()),
                        escape(vacancyDto.companyName()),
                        escape(vacancyDto.location()),
                        escape(vacancyDto.publishTime()),
                        vacancyDto.url()
                );
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
