package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import com.email.writer.dto.EmailResponse;
import com.email.writer.exception.ModelServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(EmailGeneratorService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Supported models in priority fallback order
    private static final List<String> CANDIDATE_MODELS = List.of(
            "gemini-flash-latest",
            "gemini-2.5-flash",
            "gemini-2.5-pro"
    );

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiBaseUrl;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public EmailResponse generateEmailReply(EmailRequest emailRequest) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ModelServiceException("Gemini API key is not configured on the server", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        String lastErrorReason = "No models available";

        for (String modelName : CANDIDATE_MODELS) {
            String targetUrl = geminiApiBaseUrl + "/" + modelName + ":generateContent?key=" + geminiApiKey;

            try {
                log.info("Dispatching email reply generation request to model: {}", modelName);

                String response = webClient.post()
                        .uri(targetUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(15))
                        .block();

                String replyText = extractResponseContent(response);
                if (replyText != null && !replyText.isBlank()) {
                    log.info("Generation succeeded using model: {}", modelName);
                    return EmailResponse.builder()
                            .reply(replyText)
                            .modelUsed(modelName)
                            .build();
                }

            } catch (WebClientResponseException e) {
                int statusCode = e.getStatusCode().value();
                lastErrorReason = String.format("Model %s returned HTTP %d", modelName, statusCode);
                log.warn("Gemini model {} returned status {}. Attempting fallback.", modelName, statusCode);

                // Failover on rate limits (429), temporary overload (503), or deprecated model endpoints (404)
                if (statusCode == 429 || statusCode == 503 || statusCode == 404) {
                    try {
                        // Jitter backoff between 200ms and 500ms
                        long jitter = 200 + (long) (Math.random() * 300);
                        Thread.sleep(jitter);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ModelServiceException("Request thread interrupted during failover backoff", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    continue;
                }

                // If it's a 400 Bad Request or 401 Unauthorized, do not cycle through other models
                if (statusCode == 400 || statusCode == 401 || statusCode == 403) {
                    throw new ModelServiceException("Upstream model service client error: " + e.getStatusText(), HttpStatus.valueOf(statusCode));
                }

            } catch (Exception e) {
                lastErrorReason = "Model " + modelName + " call failed: " + e.getMessage();
                log.warn("Error calling {}: {}. Attempting fallback.", modelName, e.getMessage());
            }
        }

        log.error("All candidate Gemini models failed. Last reason: {}", lastErrorReason);
        throw new ModelServiceException("All AI models are currently busy or unavailable. Please try again shortly.", HttpStatus.BAD_GATEWAY);
    }

    private String extractResponseContent(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode textNode = rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            return textNode.isMissingNode() ? null : textNode.asText();
        } catch (Exception e) {
            log.error("Failed to parse response JSON from Gemini API: {}", e.getMessage());
            throw new ModelServiceException("Failed to parse response from model provider", HttpStatus.BAD_GATEWAY);
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        String tone = (emailRequest.getTone() != null && !emailRequest.getTone().isBlank())
                ? emailRequest.getTone().toLowerCase()
                : "professional";

        // Prompt-injection protection: treat email body strictly as delimited untrusted data
        return "You are an email assistant. Generate a concise, natural reply to the email delimited below.\n"
                + "Do not include subject lines or placeholders unless necessary.\n"
                + "Target tone: " + tone + ".\n\n"
                + "=== BEGIN UNTRUSTED EMAIL CONTENT ===\n"
                + emailRequest.getEmailContent() + "\n"
                + "=== END UNTRUSTED EMAIL CONTENT ===\n\n"
                + "Reply:";
    }
}
