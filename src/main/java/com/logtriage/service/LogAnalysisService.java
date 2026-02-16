package com.logtriage.service;

import com.logtriage.ai.HuggingFaceAiClient;
import com.logtriage.engine.RuleBasedLogAnalyzer;
import com.logtriage.model.AiInsight;
import com.logtriage.model.LogAnalysisResponse;
import com.logtriage.ticket.TicketFormatter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogAnalysisService {

    private final RuleBasedLogAnalyzer ruleAnalyzer = new RuleBasedLogAnalyzer();
    private final HuggingFaceAiClient hfClient;
    private final TicketFormatter ticketFormatter = new TicketFormatter();

    public LogAnalysisService(HuggingFaceAiClient hfClient) {
        this.hfClient = hfClient;
    }

    public LogAnalysisResponse analyze(String log) {

        // 1) Always compute rule-based baseline
        LogAnalysisResponse r = ruleAnalyzer.analyze(log);

        // Defaults
        r.setAiUsed(false);
        r.setAiProvider("huggingface");
        r.setAiError(null);
        r.setAiLatencyMs(null);

        // 2) AI enrichment
        try {
            long t0 = System.nanoTime();
            AiInsight ai = hfClient.analyze(log);
            long t1 = System.nanoTime();
            r.setAiLatencyMs((t1 - t0) / 1_000_000);

            r.setAiUsed(true);

            // enrich cause
            if (ai.getAiLikelyCause() != null && !ai.getAiLikelyCause().isBlank()) {
                r.setPossibleRootCause(ai.getAiLikelyCause().trim());
            }

            // enrich steps (append, then cap to 5 total)
            if (ai.getAiNextSteps() != null && !ai.getAiNextSteps().isEmpty()) {
                r.getNextSteps().addAll(ai.getAiNextSteps());
            }
            capNextSteps(r, 5);



            String aiSummary = (ai.getAiSummary() == null || ai.getAiSummary().isBlank())
                    ? null
                    : ai.getAiSummary().trim();

            // 3) Title strategy
            r.setTicketTitle(buildTitle(r));

            // 4) Ticket body (formatted)
            String body = ticketFormatter.format(r);
            if (aiSummary != null) {
                body = "AI Summary:\n- " + aiSummary + "\n\n" + body;
            }
            r.setTicketBody(body);

            // 5) Suggested greps
            r.setSuggestedGrepQueries(TicketFormatter.buildSuggestedGrepQueries(r, "app.log"));

            return r;

        } catch (Exception e) {
            r.setAiUsed(false);
            r.setAiError(e.getMessage());


            r.setTicketTitle(buildTitle(r));
            r.setTicketBody(ticketFormatter.format(r));
            r.setSuggestedGrepQueries(TicketFormatter.buildSuggestedGrepQueries(r, "app.log"));
            return r;
        }
    }

    private void capNextSteps(LogAnalysisResponse r, int max) {
        List<String> steps = r.getNextSteps();
        if (steps == null) return;
        if (steps.size() > max) {
            r.setNextSteps(steps.subList(0, max));
        }
    }

    private String buildTitle(LogAnalysisResponse r) {
        String sev = (r.getSeverity() == null) ? "UNKNOWN" : r.getSeverity();

        String issues = String.join(" | ", r.getDetectedIssues() == null ? List.of() : r.getDetectedIssues()).toLowerCase();

        if (issues.contains("database")) return "Incident: Database connectivity issue (" + sev + ")";
        if (issues.contains("timeout")) return "Incident: Timeout / downstream latency (" + sev + ")";
        if (issues.contains("nullpointer")) return "Incident: NullPointerException (" + sev + ")";
        return "Incident: Application error (" + sev + ")";
    }

    private void normalizeNextSteps(LogAnalysisResponse r, int max) {
        List<String> steps = r.getNextSteps();
        if (steps == null) return;

        // trim + remove blanks
        List<String> cleaned = new java.util.ArrayList<>();
        for (String s : steps) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isBlank()) cleaned.add(t);
        }

        // prefer shorter, actionable items (sort by length asc, keep order for ties)
        cleaned.sort((a, b) -> {
            int la = a.length(), lb = b.length();
            if (la != lb) return Integer.compare(la, lb);
            return 0;
        });

        // dedupe while preserving sorted order
        List<String> deduped = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(cleaned));

        // cap
        if (deduped.size() > max) deduped = deduped.subList(0, max);

        // ensure exactly max items (fill if needed)
        while (deduped.size() < max) {
            deduped.add("Check logs around the first error timestamp (±5 minutes) and correlate related entries.");
        }

        r.setNextSteps(deduped);
    }

}
