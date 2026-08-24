package dev.arsonist.vacanciesmonitoring.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@Document("vacancies")
@CompoundIndex(
        name = "job_board_title_company_name_idx",
        def = "{'companyName': 1, 'jobBoard': 1, 'location': 1, 'title': 1}", unique = true)
public class Vacancy {

    @Id
    private String id;

    private String companyName;
    private JobBoard jobBoard;
    private String location;
    private String title;

    private String publishTime;
    private String url;
}
