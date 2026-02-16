package com.logtriage.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiInsight {
    private String aiSummary;
    private String aiLikelyCause;
    private List<String> aiNextSteps;
}
