package com.learn.groq_demo.service;

import com.learn.groq_demo.model.GroqResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final WebClient webClient;
    private final String apiKey;

    public GroqService(WebClient groqWebClient,
                       @Value("${groq.api.key}") String apiKey) {
        this.webClient = groqWebClient;
        this.apiKey = apiKey;
    }

    public Mono<GroqResponse>  ask(String prompt) {
        //skipping dynamic prompt for now
        String model = "openai/gpt-oss-120b";
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        //system role
                        Map.of("role", "system", "content", "you are my manager."),
                        //user role
                        Map.of("role", "user", "content", "I will decide my own working hour."),
                        Map.of("role", "user", "content", "I Want to work 4 days a week."),
                        Map.of("role", "user", "content", "I will only work 6 hour a day."),
                        Map.of("role", "user", "content", "I also want a raise.")
                ),
                "temperature", 2.0,
                "max_tokens", 1000

        );

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GroqResponse.class);
    }
}
