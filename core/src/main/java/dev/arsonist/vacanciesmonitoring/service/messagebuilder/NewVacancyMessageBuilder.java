package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewVacancyMessageBuilder implements MessageBuilder<List<Vacancy>> {

    @Override
    public String build(List<Vacancy> vacancies) {
        String body = vacancies.stream()
                .map(this::formatVacancy)
                .collect(Collectors.joining("\n\n"));

        return "<b>🫪🔥New Vacancy🔥🫪</b>\n\n" + body;
    }

    private String formatVacancy(Vacancy vacancy) {
        return """
                <b>Board:</b> %s
                <b>Title:</b> %s
                <b>Company:</b> %s
                <b>Location:</b> %s
                <b>Published:</b> %s
                <a href="%s">Link</a>
                """
                .formatted(
                        escape(vacancy.getJobBoard().name()),
                        escape(vacancy.getTitle()),
                        escape(vacancy.getCompanyName()),
                        escape(vacancy.getLocation()),
                        escape(vacancy.getPublishTime()),
                        vacancy.getUrl()
                );
    }
}
