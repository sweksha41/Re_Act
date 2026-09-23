package com.learn.ai_react.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GroqClientConfig {

    @Bean
    public WebClient groqWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

}
