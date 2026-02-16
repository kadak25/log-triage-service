package com.logtriage.controller;

import com.logtriage.model.LogAnalysisRequest;
import com.logtriage.model.LogAnalysisResponse;
import com.logtriage.service.LogAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogAnalysisService service;

    public LogAnalysisController(LogAnalysisService service) {
        this.service = service;
    }

    //  Paste / JSON
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LogAnalysisResponse analyze(@Valid @RequestBody LogAnalysisRequest request) {
        return service.analyze(request.getLogContent());
    }

    //  File upload (txt/log)
    @PostMapping(value = "/analyze/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponse analyzeFile(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(filename.endsWith(".log") || filename.endsWith(".txt"))) {
            throw new IllegalArgumentException("Only .log or .txt files are supported.");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read file content.");
        }

        return service.analyze(content);
    }
}
