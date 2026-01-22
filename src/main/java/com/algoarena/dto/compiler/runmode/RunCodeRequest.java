// src/main/java/com/algoarena/dto/compiler/runmode/RunCodeRequest.java
package com.algoarena.dto.compiler.runmode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class RunCodeRequest {
    
    @NotBlank(message = "Code is required")
    @Size(max = 15000, message = "Code must not exceed 15,000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    private String language; // "java", "cpp", "python", "javascript"

    @NotEmpty(message = "At least one test case required")
    @Size(min = 1, max = 5, message = "You can run between 1 and 5 test cases")
    private List<RunTestCaseInput> testCases;

    // Constructors
    public RunCodeRequest() {}

    public RunCodeRequest(String code, String language, List<RunTestCaseInput> testCases) {
        this.code = code;
        this.language = language;
        this.testCases = testCases;
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

    public List<RunTestCaseInput> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<RunTestCaseInput> testCases) {
        this.testCases = testCases;
    }
}