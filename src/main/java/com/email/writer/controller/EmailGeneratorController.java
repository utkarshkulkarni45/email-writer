package com.email.writer.controller;

import com.email.writer.dto.EmailRequest;
import com.email.writer.dto.EmailResponse;
import com.email.writer.service.EmailGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@CrossOrigin(
        origins = {
                "https://gmail-genie-ai-nine.vercel.app",
                "https://mail.google.com",
                "http://localhost:5173",
                "http://localhost:3000"
        },
        allowedHeaders = "*",
        exposedHeaders = {"X-Model-Used", "X-RateLimit-Limit", "X-RateLimit-Remaining"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GmailGenie API service is healthy and operational.");
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailGeneratorService.generateEmailReply(emailRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Model-Used", response.getModelUsed());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.getReply());
    }
}
