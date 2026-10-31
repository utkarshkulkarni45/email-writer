package com.email.writer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Email content cannot be blank")
    @Size(max = 5000, message = "Email content cannot exceed 5000 characters")
    private String emailContent;

    @Pattern(
            regexp = "(?i)^(professional|friendly|casual|persuasive|urgent)?$",
            message = "Tone must be one of: professional, friendly, casual, persuasive, urgent"
    )
    private String tone;
}
