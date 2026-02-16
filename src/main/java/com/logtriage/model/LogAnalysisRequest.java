package com.logtriage.model;

import jakarta.validation.constraints.NotBlank;

public class LogAnalysisRequest {

    @NotBlank
    private String logContent;

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }
}
