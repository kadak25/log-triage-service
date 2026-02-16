package com.logtriage.ticket;

import com.logtriage.model.ErrorSignature;
import com.logtriage.model.LogAnalysisResponse;

import java.util.List;
import java.util.stream.Collectors;

public class TicketFormatter {

    public String format(LogAnalysisResponse r) {
        StringBuilder sb = new StringBuilder();

        // Summary
        sb.append("Summary:\n")
                .append("- ").append(nullSafe(r.getPossibleRootCause())).append("\n\n");

        // Top error signatures
        sb.append("Top error signatures:\n");
        List<ErrorSignature> sigs = r.getTopErrorSignatures();
        if (sigs == null || sigs.isEmpty()) {
            sb.append("- (none)\n\n");
        } else {
            for (ErrorSignature s : sigs) {
                sb.append("- ")
                        .append(nullSafe(s.getExceptionType()))
                        .append(" (").append(s.getCount()).append(" occurrences)");
                if (s.getMessage() != null && !s.getMessage().isBlank()) {
                    sb.append(" | example: ").append(s.getMessage());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Detected IDs (optional)
        if (r.getDetectedIds() != null && !r.getDetectedIds().isEmpty()) {
            sb.append("Detected IDs:\n");
            for (String id : r.getDetectedIds()) {
                sb.append("- ").append(id).append("\n");
            }
            sb.append("\n");
        }

        // Recommended next steps
        sb.append("Recommended next steps:\n");
        if (r.getNextSteps() == null || r.getNextSteps().isEmpty()) {
            sb.append("- (none)\n\n");
        } else {
            for (String step : r.getNextSteps()) {
                sb.append("- ").append(step).append("\n");
            }
            sb.append("\n");
        }

        // Context to request (standard support checklist)
        sb.append("Context to request:\n")
                .append("- Timestamp range (+/- 5 min)\n")
                .append("- Request ID / Correlation ID (if available)\n")
                .append("- Environment (prod/stage) and deployment version\n");

        return sb.toString();
    }

    public static List<String> buildSuggestedGrepQueries(LogAnalysisResponse r, String filename) {
        String file = (filename == null || filename.isBlank()) ? "app.log" : filename;

        List<String> base = List.of(
                "grep -n \"ERROR\" " + file,
                "grep -n \"WARN\" " + file
        );

        List<String> bySigs = (r.getTopErrorSignatures() == null) ? List.of() :
                r.getTopErrorSignatures().stream()
                        .map(s -> "grep -n \"" + safeGrep(s.getExceptionType()) + "\" " + file)
                        .distinct()
                        .collect(Collectors.toList());

        List<String> byIds = (r.getDetectedIds() == null) ? List.of() :
                r.getDetectedIds().stream()
                        .map(id -> "grep -n \"" + safeGrep(id) + "\" " + file)
                        .distinct()
                        .collect(Collectors.toList());

        // final hint
        List<String> hint = List.of(
                "# Tip: start from the first seen timestamp and inspect ±5 minutes window",
                "# Example: grep -n \"2026-\" " + file + " | head -n 200"
        );

        return concat(base, bySigs, byIds, hint);
    }

    private static List<String> concat(List<String> a, List<String> b, List<String> c, List<String> d) {
        return new java.util.ArrayList<>() {{
            addAll(a); addAll(b); addAll(c); addAll(d);
        }};
    }

    private static String nullSafe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String safeGrep(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
