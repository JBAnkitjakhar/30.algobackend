// src/main/java/com/algoarena/dto/compiler/SubmitCodeRequest.java
package com.algoarena.dto.compiler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitCodeRequest {
    @NotBlank(message = "Code is required")
    @Size(max = 10000, message = "Code must not exceed 10,000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    private String language;

    // Constructors, Getters, Setters
    public SubmitCodeRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}