package dev.arsonist.vacanciesmonitoring.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document("snapshots")
public class Snapshot {

    @Id
    private String id;
    private JobBoard jobBoard;
    private LocalDateTime fetchTime;
    private byte[] html;
    private String parserVersion;
    private ParsingStatus parsingStatus;
    private String logId;
    private boolean test;
}
