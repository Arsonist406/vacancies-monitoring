package dev.arsonist.vacanciesmonitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Random;

@Configuration
public class AppConfig {

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("telegramClient")
    public RestClient restClient(TelegramProperties telegramProperties) {
        return RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + telegramProperties.botToken())
                .build();
    }
}
