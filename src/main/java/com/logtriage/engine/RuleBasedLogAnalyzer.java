package com.logtriage.engine;

import com.logtriage.model.ErrorSignature;
import com.logtriage.model.LogAnalysisResponse;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBasedLogAnalyzer {

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("(\\w+Exception):?\\s*(.*)");

    // Common correlation/request/trace id patterns
    private static final Pattern ID_PATTERN = Pattern.compile(
            "(correlationId|correlation_id|requestId|request_id|traceId|trace_id|x-request-id|x-correlation-id)\\s*[:=]\\s*([a-zA-Z0-9\\-]{6,})",
            Pattern.CASE_INSENSITIVE
    );

    // Timestamp examples: 2026-03-18 10:16:05.987 or 2026-03-18T10:16:05
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?)"
    );

    public LogAnalysisResponse analyze(String log) {
        LogAnalysisResponse response = new LogAnalysisResponse();

        List<String> issues = new ArrayList<>();
        List<String> steps = new ArrayList<>();

        // Group by exceptionType only
        Map<String, Integer> countsByType = new HashMap<>();
        Map<String, String> exampleMsgByType = new HashMap<>();

        String safeLog = log == null ? "" : log;
        String lower = safeLog.toLowerCase();
        String[] lines = safeLog.split("\\r?\\n");

        // ---- Extract & group error signatures (by type) ----
        for (String line : lines) {
            Matcher matcher = EXCEPTION_PATTERN.matcher(line);
            if (matcher.find()) {
                String type = matcher.group(1);
                String msg = matcher.group(2);

                countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
                exampleMsgByType.putIfAbsent(type, msg);
            }
        }

        List<ErrorSignature> signatures = new ArrayList<>();
        for (Map.Entry<String, Integer> e : countsByType.entrySet()) {
            String type = e.getKey();
            int count = e.getValue();
            String example = exampleMsgByType.getOrDefault(type, "");
            signatures.add(new ErrorSignature(type, example, count));
        }

        signatures.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        response.setTopErrorSignatures(signatures);

        // ---- Rule-based classification with priority (DB > NPE > Timeout) ----
        boolean hasDbIssue = lower.contains("connection refused")
                || lower.contains("could not open connection")
                || lower.contains("sqltransientconnectionexception");

        if (hasDbIssue) {
            issues.add("Database connectivity issue detected");
            response.setSeverity("HIGH");
            if (response.getPossibleRootCause() == null) {
                response.setPossibleRootCause("Database is unreachable, credentials/network issue, or connection pool exhausted.");
            }
            steps.add("Check DB availability (host/port), credentials, and network rules.");
            steps.add("Review connection pool metrics/timeouts and recent deployment changes.");
        }

        boolean hasNpe = lower.contains("nullpointerexception");
        if (hasNpe) {
            issues.add("NullPointerException detected");
            if (response.getSeverity() == null) response.setSeverity("HIGH");
            if (response.getPossibleRootCause() == null) {
                response.setPossibleRootCause("Unexpected null value or missing null-check in the execution path.");
            }
            steps.add("Locate the first application stack trace line (your package) and identify the failing method.");
            steps.add("Check recent changes around the failing code path.");
            steps.add("Add null-checks / validation and improve logging around inputs.");
        }

        boolean hasTimeout = lower.contains("timeout") || lower.contains("timed out") || lower.contains("read timed out");
        if (hasTimeout) {
            issues.add("Timeout detected");
            if (response.getSeverity() == null) response.setSeverity("MEDIUM");
            if (response.getPossibleRootCause() == null) {
                response.setPossibleRootCause("Downstream service is slow/unreachable or timeout values are too low.");
            }
            steps.add("Identify the dependency (HTTP/DB) causing the timeout from logs.");
            steps.add("Check latency spikes and retry behavior; consider increasing timeouts if appropriate.");
        }

        if (issues.isEmpty()) {
            response.setSeverity("LOW");
            response.setPossibleRootCause("No known critical patterns detected in the provided log snippet.");
            steps.add("Provide a longer log window around the error (±50 lines) including stack trace.");
            steps.add("Share timestamp, request id/correlation id, and environment info if available.");
        }

        // ---- Suggested grep queries (exceptions + correlation/request ids + timestamp hint) ----
        List<String> grepQueries = new ArrayList<>();

        // grep by exception type
        for (ErrorSignature s : signatures) {
            grepQueries.add("grep -n \"" + s.getExceptionType() + "\" app.log");
        }

        // extract up to 3 unique IDs (keep order)
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher idMatcher = ID_PATTERN.matcher(safeLog);
        while (idMatcher.find() && ids.size() < 3) {
            ids.add(idMatcher.group(2));
        }

        for (String id : ids) {
            grepQueries.add("grep -n \"" + id + "\" app.log");
            grepQueries.add("grep -n \"ERROR\" app.log | grep \"" + id + "\"");
        }

        // timestamp hint (capture first timestamp if exists)
        Matcher tsMatcher = TIMESTAMP_PATTERN.matcher(safeLog);
        if (tsMatcher.find()) {
            String ts = tsMatcher.group(1);
            grepQueries.add("# Tip: start from the first seen timestamp and inspect ±5 minutes window");
            grepQueries.add("grep -n \"" + ts.substring(0, 10) + "\" app.log | head -n 200");
        } else {
            grepQueries.add("# Tip: filter around the incident time window (±5 min) if you have timestamps");
            grepQueries.add("# Example: grep -n \"2026-\" app.log | head -n 200");
        }

        response.setSuggestedGrepQueries(grepQueries);

        // ---- Ticket generation ----
        String title = "Incident: Log analysis result (" + response.getSeverity() + ")";
        if (hasDbIssue) title = "Incident: Database connectivity issue (HIGH)";
        else if (hasNpe) title = "Incident: NullPointerException in production (HIGH)";
        else if (hasTimeout) title = "Incident: Timeout while calling dependency (MEDIUM)";

        StringBuilder body = new StringBuilder();
        body.append("Summary: ").append(response.getPossibleRootCause()).append("\n\n");

        body.append("Top error signatures:\n");
        for (ErrorSignature s : signatures) {
            body.append("- ")
                    .append(s.getExceptionType())
                    .append(" (")
                    .append(s.getCount())
                    .append(" occurrences)");
            if (s.getMessage() != null && !s.getMessage().isBlank()) {
                body.append(" | example: ").append(s.getMessage());
            }
            body.append("\n");
        }

        if (!ids.isEmpty()) {
            body.append("\nDetected IDs:\n");
            for (String id : ids) body.append("- ").append(id).append("\n");
        }

        body.append("\nRecommended next steps:\n");
        for (String s : steps) body.append("- ").append(s).append("\n");

        body.append("\nContext to request:\n");
        body.append("- Timestamp range (+/- 5 min)\n");
        body.append("- Request ID / Correlation ID (if available)\n");
        body.append("- Environment (prod/stage) and deployment version\n");

        response.setTicketTitle(title);
        response.setTicketBody(body.toString());

        response.setDetectedIssues(issues);
        response.setNextSteps(steps);

        return response;
    }
}
