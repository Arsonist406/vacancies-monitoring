package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.TelegramProperties;
import dev.arsonist.vacanciesmonitoring.exception.TelegramException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotifier {

    private static final int TELEGRAM_MAX_MESSAGE_LENGTH = 4096;

    private final RestClient telegramClient;
    private final TelegramProperties telegramProperties;

    public void notify(String message) {
        try {
            execute(message);
        } catch (Exception e) {
            log.error("[ID: {}] - Exception during sending notification to telegram", LogContext.getLogId(), e);
            throw new TelegramException(e);
        }
    }

    private void execute(String message) {
        var body = new HashMap<>();
        body.put("chat_id", telegramProperties.chatId());
        body.put("text", truncate(message));
        body.put("parse_mode", "HTML");

        telegramClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String truncate(String message) {
        if (message.length() <= TELEGRAM_MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, TELEGRAM_MAX_MESSAGE_LENGTH - 3) + "...";
    }
}
