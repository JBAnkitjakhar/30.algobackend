// src/main/java/com/algoarena/dto/compiler/submitmode/SubmitCodeResponse.java
package com.algoarena.dto.compiler.submitmode;

public class SubmitCodeResponse {
    
    private boolean success;
    private String verdict; // "ACCEPTED", "WRONG_ANSWER", "TLE"
    private String message;
    private int passedTestCases;
    private int totalTestCases;
    private ExecutionMetrics metrics;
    private FirstFailureDetail firstFailure; // null if ACCEPTED
    private String approachId;

    public static class ExecutionMetrics {
        private Long runtime; // milliseconds from Piston wall_time
        private Double memory; // MB from Piston

        public ExecutionMetrics() {}

        public ExecutionMetrics(Long runtime, Double memory) {
            this.runtime = runtime;
            this.memory = memory;
        }

        public Long getRuntime() { return runtime; }
        public void setRuntime(Long runtime) { this.runtime = runtime; }
        
        public Double getMemory() { return memory; }
        public void setMemory(Double memory) { this.memory = memory; }
    }

    public static class FirstFailureDetail {
        private int testCaseId;
        private String input;
        private String expectedOutput;
        private String userOutput;
        private String error; // For compilation/runtime errors

        public FirstFailureDetail() {}

        public FirstFailureDetail(int testCaseId, String input, String expectedOutput, 
                                   String userOutput, String error) {
            this.testCaseId = testCaseId;
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.userOutput = userOutput;
            this.error = error;
        }

        public int getTestCaseId() { return testCaseId; }
        public void setTestCaseId(int testCaseId) { this.testCaseId = testCaseId; }
        
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        
        public String getUserOutput() { return userOutput; }
        public void setUserOutput(String userOutput) { this.userOutput = userOutput; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public SubmitCodeResponse() {}

    public SubmitCodeResponse(boolean success, String verdict, String message) {
        this.success = success;
        this.verdict = verdict;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getPassedTestCases() { return passedTestCases; }
    public void setPassedTestCases(int passedTestCases) { this.passedTestCases = passedTestCases; }
    
    public int getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }
    
    public ExecutionMetrics getMetrics() { return metrics; }
    public void setMetrics(ExecutionMetrics metrics) { this.metrics = metrics; }
    
    public FirstFailureDetail getFirstFailure() { return firstFailure; }
    public void setFirstFailure(FirstFailureDetail firstFailure) { this.firstFailure = firstFailure; }
    
    public String getApproachId() { return approachId; }
    public void setApproachId(String approachId) { this.approachId = approachId; }
}