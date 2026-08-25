package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.dto.FastApiError;
import dev.arsonist.vacanciesmonitoring.service.LogContext;
import org.springframework.stereotype.Component;

@Component
public class FastApiErrorMessageBuilder {

    public String build(FastApiError error) {
        return """
                    <b>🐍FAST API ERROR🐍</b>

                    <b>Code:</b> %s
                    <b>Snapshot ID:</b> %s
                    <b>Parser Version:</b> %s
                    <b>Timestamp:</b> %s
                    <b>Log ID:</b> %s

                    <b>Message:</b>
                    <blockquote expandable><code>%s</code></blockquote>
                   """
                .formatted(
                        error.code(),
                        escape(error.snapshotId()),
                        escape(error.parserVersion()),
                        escape(error.timestamp()),
                        escape(error.message()),
                        escape(LogContext.getLogId())
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
