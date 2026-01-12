//src/main/java/com/algoarena/dto/complexity/ComplexityAnalysisRequest.java

package com.algoarena.dto.complexity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComplexityAnalysisRequest {
    
    @NotBlank(message = "Code is required")
    @Size(min = 10, max = 10000, message = "Code must be between 10 and 10000 characters")
    private String code;
    
    private String language; // Optional
    
    // Constructors
    public ComplexityAnalysisRequest() {
    }
    
    public ComplexityAnalysisRequest(String code, String language) {
        this.code = code;
        this.language = language;
    }
    
    // Getters and Setters
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