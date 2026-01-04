// src/main/java/com/algoarena/dto/compiler/QuestionExecutionRequest.java
package com.algoarena.dto.compiler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionExecutionRequest {

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Code is required")
    @Size(max = 100000, message = "Code must not exceed 100,000 characters")
    private String code;

    private String version; // Optional, defaults to latest

    // Constructors
    public QuestionExecutionRequest() {}

    public QuestionExecutionRequest(String language, String code) {
        this.language = language;
        this.code = code;
    }

    // Getters and Setters
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}