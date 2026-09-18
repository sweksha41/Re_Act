package com.learn.groq_demo.controller;

import com.learn.groq_demo.model.GroqResponse;
import com.learn.groq_demo.service.GroqService;
import com.learn.groq_demo.service.PromptEngineeringService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/groq")
public class GroqController {

    private final GroqService groqService;
    private final PromptEngineeringService promptService;

    public GroqController(GroqService groqService,
                          PromptEngineeringService promptService
    ) {
        this.groqService = groqService;
        this.promptService = promptService;
    }

    @GetMapping("/ask")
    public Mono<GroqResponse> ask(@RequestParam String prompt) {
        return groqService.ask(prompt);
    }

    @GetMapping("/promptEngine")
    public Mono<String> promptEngine(@RequestParam String prompt) {
        return promptService.promptEngine(prompt);
    }
}
