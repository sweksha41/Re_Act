
package com.learn.ai_react.model;

import java.util.List;

public record GroqResponse(
        List<Choice> choices,
        Usage usage
) {}
