package com.learn.ai_react.controller;

import com.learn.ai_react.service.ShoppingAgentService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agent")
public class ShoppingAgentController {

    private final ShoppingAgentService shoppingAgentService;

    public ShoppingAgentController(ShoppingAgentService shoppingAgentService) {
        this.shoppingAgentService = shoppingAgentService;
    }

    @PostMapping("/run")
    public Mono<String> run(@RequestParam String question) {
        return shoppingAgentService.runAgent(question);
    }
}