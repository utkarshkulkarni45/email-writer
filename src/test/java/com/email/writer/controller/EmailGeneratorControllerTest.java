package com.email.writer.controller;

import com.email.writer.dto.EmailRequest;
import com.email.writer.dto.EmailResponse;
import com.email.writer.service.EmailGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailGeneratorController.class)
@Import(EmailGeneratorControllerTest.StubServiceConfig.class)
class EmailGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubEmailGeneratorService emailGeneratorService;

    @BeforeEach
    void resetStub() {
        emailGeneratorService.calls = 0;
    }

    @Test
    void returnsReplyAndModelHeader() throws Exception {
        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\":\"Any update?\",\"tone\":\"professional\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Model-Used", "gemini-flash-latest"))
                .andExpect(content().string("Thanks for the update."));

        assertEquals(1, emailGeneratorService.calls);
    }

    @Test
    void rejectsBlankEmailContent() throws Exception {
        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\":\"   \",\"tone\":\"professional\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email content cannot be blank"));

        assertEquals(0, emailGeneratorService.calls);
    }

    @Test
    void rejectsUnknownTone() throws Exception {
        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\":\"Hello\",\"tone\":\"sarcastic\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Tone must be one of: professional, friendly, casual, persuasive, urgent"));

        assertEquals(0, emailGeneratorService.calls);
    }

    @Test
    void rejectsContentOverTheLengthLimit() throws Exception {
        String longContent = "a".repeat(5001);

        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\":\"" + longContent + "\",\"tone\":\"professional\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email content cannot exceed 5000 characters"));

        assertEquals(0, emailGeneratorService.calls);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/email/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed Request"));
    }

    @TestConfiguration
    static class StubServiceConfig {
        @Bean
        StubEmailGeneratorService emailGeneratorService() {
            return new StubEmailGeneratorService();
        }
    }

    static class StubEmailGeneratorService extends EmailGeneratorService {
        private int calls;

        StubEmailGeneratorService() {
            super(WebClient.builder());
        }

        @Override
        public EmailResponse generateEmailReply(EmailRequest emailRequest) {
            calls++;
            return EmailResponse.builder()
                    .reply("Thanks for the update.")
                    .modelUsed("gemini-flash-latest")
                    .build();
        }
    }
}
