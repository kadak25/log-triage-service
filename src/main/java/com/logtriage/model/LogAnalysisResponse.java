package com.logtriage.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LogAnalysisResponse {

    private String severity;
    private List<String> detectedIssues = new ArrayList<>();
    private String possibleRootCause;
    private List<String> nextSteps = new ArrayList<>();

    private List<ErrorSignature> topErrorSignatures = new ArrayList<>();
    private List<String> detectedIds = new ArrayList<>();

    private String ticketTitle;
    private String ticketBody;

    private List<String> suggestedGrepQueries = new ArrayList<>();

    // AI meta
    private boolean aiUsed;
    private String aiProvider;
    private String aiError;
    private Long aiLatencyMs;
}
