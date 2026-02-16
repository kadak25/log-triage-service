package com.logtriage.ai;

import com.logtriage.model.AiInsight;

import java.util.List;

public class AiLogAnalyzer {

    public AiInsight analyzeWithAi(String log) {
        // MOCK AI (ileride OpenAI / HF bağlanır)
        // Burada exception fırlatabilirsin test için
        if (log.length() > 10_000) {
            throw new RuntimeException("AI input too large");
        }

        return new AiInsight(
                "AI analysis suggests a database-related incident impacting request processing.",
                "Primary issue appears to be database connectivity instability, causing cascading timeouts and application errors.",
                List.of(
                        "Verify database health and connection pool metrics.",
                        "Check if recent deployments affected DB credentials or network rules.",
                        "Monitor retry and timeout configuration for dependent services.",
                        "Review application logs around the first occurrence timestamp."
                )
        );
    }
}
