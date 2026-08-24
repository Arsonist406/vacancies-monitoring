package dev.arsonist.vacanciesmonitoring.service;

import dev.arsonist.vacanciesmonitoring.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class TelegramNotifier {

    private final RestClient telegramClient;
    private final TelegramProperties telegramProperties;

    public void notify(String message) {
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
