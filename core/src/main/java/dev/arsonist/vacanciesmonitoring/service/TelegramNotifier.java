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
        body.put("text", message);
        body.put("parse_mode", "HTML");

        telegramClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
