// src/main/java/com/algoarena/dto/compiler/CodeExecutionResult.java
package com.algoarena.dto.compiler;

import java.util.List;

public class CodeExecutionResult {
    private boolean success;
    private String verdict; // "ACCEPTED", "WRONG_ANSWER", "TLE", "COMPILATION_ERROR"
    private String message;
    private List<TestCaseResult> testCaseResults;
    private ExecutionMetrics metrics;
    private String error;
    private Integer failedTestCaseIndex; // First failed testcase index

    public static class ExecutionMetrics {
        private Long maxTimeMs;
        private Double totalMemoryMb;
        private int totalTestCases;
        private int passedTestCases;

        public ExecutionMetrics() {}

        public Long getMaxTimeMs() { return maxTimeMs; }
        public void setMaxTimeMs(Long maxTimeMs) { this.maxTimeMs = maxTimeMs; }

        public Double getTotalMemoryMb() { return totalMemoryMb; }
        public void setTotalMemoryMb(Double totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }

        public int getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }

        public int getPassedTestCases() { return passedTestCases; }
        public void setPassedTestCases(int passedTestCases) { this.passedTestCases = passedTestCases; }
    }

    public CodeExecutionResult() {}

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<TestCaseResult> getTestCaseResults() { return testCaseResults; }
    public void setTestCaseResults(List<TestCaseResult> testCaseResults) { 
        this.testCaseResults = testCaseResults; 
    }

    public ExecutionMetrics getMetrics() { return metrics; }
    public void setMetrics(ExecutionMetrics metrics) { this.metrics = metrics; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Integer getFailedTestCaseIndex() { return failedTestCaseIndex; }
    public void setFailedTestCaseIndex(Integer failedTestCaseIndex) { 
        this.failedTestCaseIndex = failedTestCaseIndex; 
    }
}