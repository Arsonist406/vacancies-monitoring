package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

import dev.arsonist.vacanciesmonitoring.service.LogContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class CoreErrorMessageBuilder implements MessageBuilder<Exception> {

    @Override
    public String build(Exception exception) {
        return """
                    <b>🍃CORE ERROR🍃</b>

                    <b>Timestamp:</b> %s
                    <b>Log ID:</b> %s
                    <b>Message:</b> %s
                   
                    <b>Stack Trace:</b>
                    <blockquote expandable><code>%s</code></blockquote>
                   """
                .formatted(
                        LocalDateTime.now(),
                        escape(LogContext.getLogId()),
                        escape(exception.getMessage()),
                        escape(Arrays.toString(exception.getStackTrace()))
                );
    }
}
