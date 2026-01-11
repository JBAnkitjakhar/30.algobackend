// src/main/java/com/algoarena/dto/compiler/QuestionExecutionResponse.java
package com.algoarena.dto.compiler;

import java.util.List;

public class QuestionExecutionResponse {

    private boolean success;
    private String message;
    private List<TestCaseResult> testCaseResults;
    private ExecutionMetrics metrics;
    private String error; // Compile error or runtime error

    // Inner class for overall execution metrics
    public static class ExecutionMetrics {
        private Long maxTimeMs;           // Max time among all test cases (like LeetCode)
        private Double totalMemoryMb;     // Total process memory in MB
        private int totalTestCases;
        private int executedTestCases;

        public ExecutionMetrics() {}

        // Getters and Setters
        public Long getMaxTimeMs() { return maxTimeMs; }
        public void setMaxTimeMs(Long maxTimeMs) { this.maxTimeMs = maxTimeMs; }
        
        public Double getTotalMemoryMb() { return totalMemoryMb; }
        public void setTotalMemoryMb(Double totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }
        
        public int getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }
        
        public int getExecutedTestCases() { return executedTestCases; }
        public void setExecutedTestCases(int executedTestCases) { this.executedTestCases = executedTestCases; }
    }

    // Constructors
    public QuestionExecutionResponse() {}

    public QuestionExecutionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }

    public void setTestCaseResults(List<TestCaseResult> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }

    public ExecutionMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ExecutionMetrics metrics) {
        this.metrics = metrics;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}