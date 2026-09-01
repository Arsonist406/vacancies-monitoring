package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewVacancyMessageBuilder implements MessageBuilder<List<Vacancy>> {

    @Override
    public String build(List<Vacancy> vacancies) {
        return vacancies.stream()
                .map(this::formatVacancy)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatVacancy(Vacancy vacancy) {
        return """
                <b>🔥</b> %s <b>🔥</b>
                <b>Filter:</b> %s
                <b>Board:</b> %s
                <b>Company:</b> %s
                <b>Location:</b> %s
                <b>Published:</b> %s
                <a href="%s">Link</a>
                """
                .formatted(
                        escape(vacancy.getTitle()),
                        escape(vacancy.getFilter()),
                        escape(vacancy.getJobBoard().name()),
                        escape(vacancy.getCompanyName()),
                        escape(vacancy.getLocation()),
                        escape(vacancy.getPublishTime()),
                        vacancy.getUrl()
                );
    }
}
