// src/main/java/com/algoarena/dto/compiler/TestCaseResult.java
package com.algoarena.dto.compiler;

public class TestCaseResult {

    private int index;
    private String output;
    private Long timeMs;
    private String status; // "success", "error", "tle"

    // Constructors
    public TestCaseResult() {}

    public TestCaseResult(int index, String output, Long timeMs, String status) {
        this.index = index;
        this.output = output;
        this.timeMs = timeMs;
        this.status = status;
    }

    // Getters and Setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(Long timeMs) {
        this.timeMs = timeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TestCaseResult{" +
                "index=" + index +
                ", output='" + output + '\'' +
                ", timeMs=" + timeMs +
                ", status='" + status + '\'' +
                '}';
    }
}