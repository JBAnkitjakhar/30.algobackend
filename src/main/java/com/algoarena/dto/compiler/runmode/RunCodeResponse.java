// src/main/java/com/algoarena/dto/compiler/runmode/RunCodeResponse.java
package com.algoarena.dto.compiler.runmode;

import java.util.List;

public class RunCodeResponse {
    
    private boolean success;
    private String verdict; // "ACCEPTED", "WRONG_ANSWER", "TLE"
    private String message;
    private List<RunTestCaseResult> testCaseResults;
    private ExecutionMetrics metrics;

    // Simplified metrics - only what we actually use
    public static class ExecutionMetrics {
        private Double memoryUsedMb; // From Piston
        private int totalTestCases;
        private int passedTestCases;
        private int failedTestCases;
        private int tleTestCases;

        public ExecutionMetrics() {}

        // Getters and Setters
        public Double getMemoryUsedMb() { return memoryUsedMb; }
        public void setMemoryUsedMb(Double memoryUsedMb) { this.memoryUsedMb = memoryUsedMb; }
        
        public int getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }
        
        public int getPassedTestCases() { return passedTestCases; }
        public void setPassedTestCases(int passedTestCases) { this.passedTestCases = passedTestCases; }
        
        public int getFailedTestCases() { return failedTestCases; }
        public void setFailedTestCases(int failedTestCases) { this.failedTestCases = failedTestCases; }
        
        public int getTleTestCases() { return tleTestCases; }
        public void setTleTestCases(int tleTestCases) { this.tleTestCases = tleTestCases; }
    }

    // Constructors
    public RunCodeResponse() {}

    public RunCodeResponse(boolean success, String verdict, String message) {
        this.success = success;
        this.verdict = verdict;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RunTestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }

    public void setTestCaseResults(List<RunTestCaseResult> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }

    public ExecutionMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ExecutionMetrics metrics) {
        this.metrics = metrics;
    }
}