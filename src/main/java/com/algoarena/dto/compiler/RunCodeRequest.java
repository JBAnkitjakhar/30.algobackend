// src/main/java/com/algoarena/dto/compiler/RunCodeRequest.java
package com.algoarena.dto.compiler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class RunCodeRequest {
    @NotBlank(message = "Code is required")
    @Size(max = 10000, message = "Code must not exceed 10,000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    private String language;

    @NotEmpty(message = "At least one test case required")
    @Size(min = 1, max = 5, message = "Select 1-5 test cases")
    private List<Integer> testCaseIds; // User selected testcase IDs

    // Constructors, Getters, Setters
    public RunCodeRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<Integer> getTestCaseIds() { return testCaseIds; }
    public void setTestCaseIds(List<Integer> testCaseIds) { this.testCaseIds = testCaseIds; }
}