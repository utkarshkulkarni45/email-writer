package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import com.email.writer.dto.EmailResponse;
import com.email.writer.exception.ModelServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EmailGeneratorServiceTest {

    private MockWebServer mockWebServer;
    private EmailGeneratorService emailGeneratorService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        emailGeneratorService = new EmailGeneratorService(WebClient.builder());
        ReflectionTestUtils.setField(emailGeneratorService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(emailGeneratorService, "geminiApiBaseUrl", mockWebServer.url("/models").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnReplyOnFirstModelSuccess() {
        String successJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello, thanks for reaching out.\"}]}}]}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successJson));

        EmailRequest request = EmailRequest.builder()
                .emailContent("Can we meet tomorrow?")
                .tone("professional")
                .build();

        EmailResponse response = emailGeneratorService.generateEmailReply(request);

        assertNotNull(response);
        assertEquals("Hello, thanks for reaching out.", response.getReply());
        assertEquals("gemini-flash-latest", response.getModelUsed());
    }

    @Test
    void shouldFailoverToSecondModelWhenFirstModelReturns503() {
        // First model fails with 503 Service Unavailable
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("{\"error\":{\"code\":503,\"message\":\"Overloaded\"}}"));

        // Second model succeeds with 200 OK
        String successJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Sure, tomorrow works.\"}]}}]}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successJson));

        EmailRequest request = EmailRequest.builder()
                .emailContent("Are you free tomorrow?")
                .tone("friendly")
                .build();

        EmailResponse response = emailGeneratorService.generateEmailReply(request);

        assertNotNull(response);
        assertEquals("Sure, tomorrow works.", response.getReply());
        assertEquals("gemini-2.5-flash", response.getModelUsed());
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void shouldThrowBadGatewayWhenAllCandidateModelsFail() {
        // Enqueue 503 for all 3 candidate models
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Overloaded"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Overloaded"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Overloaded"));

        EmailRequest request = EmailRequest.builder()
                .emailContent("Test content")
                .tone("casual")
                .build();

        assertThrows(ModelServiceException.class, () -> emailGeneratorService.generateEmailReply(request));
    }

    @Test
    void shouldThrowImmediatelyOn400ClientErrorWithoutCyclingModels() {
        // 400 Bad Request should not trigger fallback cycling
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));

        EmailRequest request = EmailRequest.builder()
                .emailContent("Invalid prompt")
                .tone("casual")
                .build();

        assertThrows(ModelServiceException.class, () -> emailGeneratorService.generateEmailReply(request));
        assertEquals(1, mockWebServer.getRequestCount());
    }
}

