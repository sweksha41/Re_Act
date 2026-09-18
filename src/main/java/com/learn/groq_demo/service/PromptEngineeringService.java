package com.learn.groq_demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class PromptEngineeringService {

    private final WebClient webClient;
    private final String apiKey;

    public PromptEngineeringService(WebClient groqWebClient,
                                    @Value("${groq.api.key}") String apiKey) {
        this.webClient = groqWebClient;
        this.apiKey = apiKey;
    }

    public Mono<String> promptEngine(String prompt) {
        //skipping dynamic prompt for now
        String model = "openai/gpt-oss-120b";
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        //user
                        Map.of("role", "user", "content", "#ROLE:\n" +
                                "You are a support assistant at a mobile/laptop company\n" +
                                "#TASK\n" +
                                "You have to classify the issue in a category\n" +
                                "#CONSTRAINT\n" +
                                "You have to classify the issue in one of three categories namely billing, technical, return.\n" +
                                "#OUTPUT FORMAT\n" +
                                "Your answer should be in one word only. The one word shoud be one of the categories given in constraints\n" +
                                "#Example\n" +
                                "For instance if a user compalin says he wants a refund then the category is Return\n" +
                                "#FALLBACK\n" +
                                "If the issue is unrelated to any of the categories mentioned in constraints, then the answer should be OTHER\n" +
                                "This is a user complaint:\n" +
                                "My laptop is slow.")
                ),
                "temperature", 2.0,
                "max_tokens", 1000

        );

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }
}
