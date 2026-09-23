package com.learn.ai_react.service;

import com.learn.ai_react.model.GroqResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShoppingAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a shopping assistant.

            Use ONLY these tools:
            - getCurrentFuelPrice()
            - getVehicleAverage()
            - get_product_price(product)
            - calculator(expression)

            IMPORTANT:
            1. You must choose exactly ONE tool at a time.
            2. Output must follow this exact format:
               Action: tool_name(argument)
            3. Do NOT write explanations before the Action line.
            4. Allowed examples:
               Action: getCurrentFuelPrice()
               Action: getVehicleAverage()
               Action: get_product_price("iPhone 17")
               Action: calculator("500 / 15")
               Action: calculator("1000 + (500 / 15 * 3)")
            5. Never write:
               Action: get_product_price(product="iPhone 17")
               Action: calculator(expression="5000 - 1000")
            6. After each tool call, wait for the Observation.
            7. When the task is complete, respond with:
               Final Answer: ...
            8. If the buyer has travel cost, include that in total cost.
            9. If the user asks if they can buy the product, calculate:
               total_cost = product_price + travel_fuel_cost
               remaining_money = money_available - total_cost
            10. The final answer should be clear and human readable.

            Example flow:
            Action: getCurrentFuelPrice()
            Observation: 3

            Action: getVehicleAverage()
            Observation: 15

            Action: calculator("500 / 15")
            Observation: 33.33

            Action: calculator("33.33 * 3")
            Observation: 99.99

            Action: get_product_price("iPhone 17")
            Observation: 1000

            Action: calculator("1000 + (33.33 * 3)")
            Observation: 1099.99

            Action: calculator("17000 - (1000 + (33.33 * 3))")
            Observation: 15900.01
            """;

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public ShoppingAgentService(
            WebClient groqWebClient,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.model:openai/gpt-oss-120b}") String model
    ) {
        this.webClient = groqWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Mono<String> runAgent(String question) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", question));

        return runAgentStep(messages, 0);
    }

    private Mono<String> runAgentStep(List<Map<String, String>> messages, int step) {
        if (step >= 6) {
            return Mono.just("No final answer produced");
        }

        return callGroq(messages)
                .flatMap(answer -> {
                    System.out.println("STEP " + (step + 1));
                    System.out.println(answer);

                    if (answer.contains("Final Answer:")) {
                        return Mono.just(answer);
                    }

                    Optional<ToolCall> toolCall = parseToolCall(answer);

                    if (toolCall.isEmpty()) {
                        // give the model one nudge if it skipped the action format
                        List<Map<String, String>> nextMessages = new ArrayList<>(messages);
                        nextMessages.add(Map.of("role", "assistant", "content", answer));
                        nextMessages.add(Map.of(
                                "role", "user",
                                "content",
                                "Reminder: respond with exactly one tool call in the format Action: tool_name(argument)"
                        ));

                        return Mono.delay(Duration.ofSeconds(5))
                                .then(runAgentStep(nextMessages, step + 1));
                    }

                    ToolCall call = toolCall.get();
                    Object observation = executeTool(call.name(), call.argument());

                    System.out.println("Observation: " + observation);

                    List<Map<String, String>> nextMessages = new ArrayList<>(messages);
                    nextMessages.add(Map.of("role", "assistant", "content", answer));
                    nextMessages.add(Map.of("role", "user", "content", "Observation: " + observation));

                    return Mono.delay(Duration.ofSeconds(2))
                            .then(runAgentStep(nextMessages, step + 1));
                });
    }

    private Optional<ToolCall> parseToolCall(String answer) {
        Matcher matcher = Pattern
                .compile("Action\\s*:\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s*", Pattern.DOTALL)
                .matcher(answer);

        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String arg = matcher.group(2).trim();

            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
            }

            return Optional.of(new ToolCall(name, arg));
        }

        return Optional.empty();
    }

    private Object executeTool(String toolName, String toolArg) {
        Map<String, Function<String, Object>> tools = new HashMap<>();

        tools.put("getCurrentFuelPrice", s -> getCurrentFuelPrice());
        tools.put("getVehicleAverage", s -> getVehicleAverage());
        tools.put("getFuelPrice", s -> getCurrentFuelPrice());
        tools.put("getVehicleMileage", s -> getVehicleAverage());

        tools.put("get_product_price", s -> getProductPrice(toolArg));
        tools.put("calculator", s -> calculator(toolArg));

        Function<String, Object> tool = tools.get(toolName);
        if (tool == null) {
            return "Tool not found";
        }

        return tool.apply(toolArg);
    }

    private Mono<String> callGroq(List<Map<String, String>> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.0);

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .map(response -> {
                    if (response == null || response.choices() == null || response.choices().isEmpty()) {
                        return "No response from Groq";
                    }
                    return response.choices().getFirst().message().content();
                });
    }

    private Object getCurrentFuelPrice() {
        return 3;
    }

    private Object getVehicleAverage() {
        return 15;
    }

    private Object getProductPrice(String product) {
        String p = product.trim();

        if ("iPhone 17".equalsIgnoreCase(p)) {
            return 1000;
        } else if ("iPhone 15".equalsIgnoreCase(p)) {
            return 500;
        } else {
            return 0;
        }
    }

    private Object calculator(String expression) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return result;
        } catch (Exception e) {
            return "calc error!";
        }
    }

    private record ToolCall(String name, String argument) {}
}