// src/main/java/com/algoarena/dto/compiler/submitmode/SubmitTestCaseResult.java
package com.algoarena.dto.compiler.submitmode;

public class SubmitTestCaseResult {
    
    private int id;
    private String status; // "PASS", "FAIL", "TLE"
    private String expectedOutput;
    private String userOutput;
    private String error;
    private Long executionTime; // ✅ NEW - milliseconds

    public SubmitTestCaseResult() {}

    public SubmitTestCaseResult(int id, String status) {
        this.id = id;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    
    public String getUserOutput() { return userOutput; }
    public void setUserOutput(String userOutput) { this.userOutput = userOutput; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    // ✅ NEW
    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
}