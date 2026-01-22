// src/main/java/com/algoarena/dto/compiler/runmode/RunTestCaseResult.java
package com.algoarena.dto.compiler.runmode;

public class RunTestCaseResult {
    
    private int id;
    private String status; // "PASS", "FAIL", "TLE"
    private String expectedOutput;
    private String userOutput;
    private String error; // Only for FAIL or TLE

    // Constructors
    public RunTestCaseResult() {}

    public RunTestCaseResult(int id, String status) {
        this.id = id;
        this.status = status;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getUserOutput() {
        return userOutput;
    }

    public void setUserOutput(String userOutput) {
        this.userOutput = userOutput;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}