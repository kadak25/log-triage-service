package com.logtriage.engine;

import com.logtriage.model.LogAnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedLogAnalyzerTest {

    @Test
    void shouldDetectNpeAndSetHighSeverity() {
        RuleBasedLogAnalyzer analyzer = new RuleBasedLogAnalyzer();

        String log = "java.lang.NullPointerException at com.myapp.Service.process(Service.java:42)";
        LogAnalysisResponse res = analyzer.analyze(log);

        assertEquals("HIGH", res.getSeverity());
        assertTrue(res.getDetectedIssues().stream().anyMatch(s -> s.contains("NullPointerException")));
        assertNotNull(res.getTicketTitle());
        assertNotNull(res.getTicketBody());
        assertFalse(res.getSuggestedGrepQueries().isEmpty());
    }

    @Test
    void shouldGroupSameExceptionTypeCounts() {
        RuleBasedLogAnalyzer analyzer = new RuleBasedLogAnalyzer();

        String log = "java.lang.NullPointerException at A\njava.lang.NullPointerException at B";
        LogAnalysisResponse res = analyzer.analyze(log);

        assertNotNull(res.getTopErrorSignatures());
        assertTrue(
                res.getTopErrorSignatures().stream()
                        .anyMatch(sig -> sig.getExceptionType().contains("NullPointerException") && sig.getCount() == 2)
        );
    }
}
