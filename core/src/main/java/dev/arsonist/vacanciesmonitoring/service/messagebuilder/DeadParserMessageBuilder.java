package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import dev.arsonist.vacanciesmonitoring.service.LogContext;
import org.springframework.stereotype.Component;

@Component
public class DeadParserMessageBuilder implements MessageBuilder<Snapshot> {

    @Override
    public String build(Snapshot snapshot) {
        return """
                    <b>💀DEAD PARSER💀</b>

                    <b>Snapshot ID:</b> %s
                    <b>Job Board:</b> %s
                    <b>Parser Version:</b> %s
                    <b>Fetch Time:</b> %s
                    <b>Log ID:</b> %s
                   """
                .formatted(
                        escape(snapshot.getId()),
                        escape(snapshot.getJobBoard().name()),
                        escape(snapshot.getParserVersion()),
                        escape(snapshot.getFetchTime().toString()),
                        escape(LogContext.getLogId())
                );
    }
}
