package com.logtriage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logtriage.model.AiInsight;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class HuggingFaceAiClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String routerModel;
    private final Duration timeout;

    public HuggingFaceAiClient(
            WebClient.Builder builder,
            @Value("${hf.router-model:meta-llama/Meta-Llama-3-8B-Instruct}") String routerModel,
            @Value("${hf.timeout-seconds:25}") int timeoutSeconds
    ) {
        this.routerModel = routerModel;
        this.timeout = Duration.ofSeconds(timeoutSeconds);

        this.webClient = builder
                .baseUrl("https://router.huggingface.co/v1")
                .build();
    }

    public AiInsight analyze(String logContent) {
        String token = System.getenv("HF_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("HF_TOKEN env var is missing. Set HF_TOKEN=hf_xxx");
        }

        String prompt = buildPrompt(logContent);

        // OpenAI-compatible payload (HF Router)
        String payload = """
        {
          "model": %s,
          "messages": [
            { "role": "user", "content": %s }
          ],
          "temperature": 0.2,
          "max_tokens": 320
        }
        """.formatted(toJsonString(routerModel), toJsonString(prompt));

        String raw = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "HF HTTP " + resp.statusCode().value() + " - " + compact(body)
                                )))
                )
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();

        String generated = extractContent(raw);
        return parseInsight(generated);
    }

    private String extractContent(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);

            // Router error format (sometimes)
            if (root.has("error")) {
                throw new RuntimeException("HF error: " + root.get("error").toString());
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("HF router response has no choices: " + compact(rawJson));
            }

            return choices.get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse HF router response: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String logContent) {
        return """
You are a Production Support Engineer.

Analyze the following log snippet and return ONLY in the exact format below.
Do NOT add explanations, markdown, or any text outside this format.

FORMAT:
SUMMARY: <1-2 sentences>
LIKELY_CAUSE: <1 sentence>
NEXT_STEPS:
- <step 1>
- <step 2>
- <step 3>
- <step 4>
- <step 5>

RULES:
- Do NOT invent fake names (e.g., "method A", "ClassX") if they do not appear in the log.
- If real class or method names are missing, speak generically (e.g., "a null reference in application code").
- Be concise, practical, and production-focused.
- Always return exactly 5 NEXT_STEPS.

LOG:
%s
""".formatted(
                logContent.length() > 6000
                        ? logContent.substring(0, 6000)
                        : logContent
        );
    }


    private AiInsight parseInsight(String text) {
        String summary = "";
        String cause = "";
        List<String> steps = new ArrayList<>();

        boolean inSteps = false;
        for (String line : (text == null ? "" : text).split("\\r?\\n")) {
            String l = line.trim();
            if (l.startsWith("SUMMARY:")) summary = l.substring("SUMMARY:".length()).trim();
            else if (l.startsWith("LIKELY_CAUSE:")) cause = l.substring("LIKELY_CAUSE:".length()).trim();
            else if (l.startsWith("NEXT_STEPS:")) inSteps = true;
            else if (inSteps && l.startsWith("-")) {
                String step = l.substring(1).trim();
                if (!step.isBlank()) steps.add(step);
            }
        }

        if (summary.isBlank()) summary = "AI-generated incident summary.";
        if (cause.isBlank()) cause = "Likely caused by an application or dependency failure.";

        // dedupe + cap
        List<String> deduped = new ArrayList<>(new LinkedHashSet<>(steps));
        if (deduped.isEmpty()) {
            deduped.add("Check the first error occurrence timestamp and inspect ±5 minutes around it.");
            deduped.add("Confirm recent deployments/changes and service health.");
            deduped.add("Validate downstream dependencies (DB/HTTP) and timeout/retry settings.");
        }
        if (deduped.size() > 6) deduped = deduped.subList(0, 6);

        return new AiInsight(summary, cause, deduped);
    }

    private String toJsonString(String s) {
        try {
            return mapper.writeValueAsString(s);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    private String compact(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}
