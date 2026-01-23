// src/main/java/com/algoarena/dto/compiler/submitmode/SubmitCodeRequest.java
package com.algoarena.dto.compiler.submitmode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitCodeRequest {
    
    @NotBlank(message = "Code is required")
    @Size(max = 15000, message = "Code must not exceed 15,000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    private String language; // "java", "cpp", "python", "javascript"

    public SubmitCodeRequest() {}

    public SubmitCodeRequest(String code, String language) {
        this.code = code;
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}