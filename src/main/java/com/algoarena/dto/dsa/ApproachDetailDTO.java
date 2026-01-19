// src/main/java/com/algoarena/dto/dsa/ApproachDetailDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.UserApproaches.ApproachData;
import com.algoarena.model.UserApproaches.ApproachStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ApproachDetailDTO {

    private String id;
    private String questionId;
    private String userId;
    private String userName;

    @NotBlank(message = "Text content is required")
    @Size(min = 10, max = 15000, message = "Text content must be between 10 and 15000 characters")
    private String textContent;

    @Size(max = 5000, message = "Code content must not exceed 5000 characters")
    private String codeContent;

    @NotNull(message = "Code language is required")
    @Pattern(regexp = "^(java|python|javascript|cpp|c|csharp|go|rust|kotlin|swift|ruby|php|typescript)$", message = "Invalid programming language")
    private String codeLanguage;

    private ApproachStatus status;
    private Long runtime;
    private Long memory;
    private ComplexityAnalysisDTO complexityAnalysis;
    private TestcaseFailureDTO wrongTestcase;
    private TestcaseFailureDTO tleTestcase;

    private int contentSize;
    private double contentSizeKB;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ⭐ ADD THIS: Inner ComplexityAnalysisDTO class
    public static class ComplexityAnalysisDTO {
        private String timeComplexity;
        private String spaceComplexity;
        private String complexityDescription;

        public ComplexityAnalysisDTO() {
        }

        public ComplexityAnalysisDTO(String timeComplexity, String spaceComplexity, String complexityDescription) {
            this.timeComplexity = timeComplexity;
            this.spaceComplexity = spaceComplexity;
            this.complexityDescription = complexityDescription;
        }

        public String getTimeComplexity() {
            return timeComplexity;
        }

        public void setTimeComplexity(String timeComplexity) {
            this.timeComplexity = timeComplexity;
        }

        public String getSpaceComplexity() {
            return spaceComplexity;
        }

        public void setSpaceComplexity(String spaceComplexity) {
            this.spaceComplexity = spaceComplexity;
        }

        public String getComplexityDescription() {
            return complexityDescription;
        }

        public void setComplexityDescription(String complexityDescription) {
            this.complexityDescription = complexityDescription;
        }
    }

    public static class TestcaseFailureDTO {
        private String input;
        private String userOutput;
        private String expectedOutput;

        public TestcaseFailureDTO() {
        }

        public TestcaseFailureDTO(String input, String userOutput, String expectedOutput) {
            this.input = input;
            this.userOutput = userOutput;
            this.expectedOutput = expectedOutput;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getUserOutput() {
            return userOutput;
        }

        public void setUserOutput(String userOutput) {
            this.userOutput = userOutput;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }

    public ApproachDetailDTO() {
    }

    public ApproachDetailDTO(ApproachData data, String userId, String userName) {
        this.id = data.getId();
        this.questionId = data.getQuestionId();
        this.userId = userId;
        this.userName = userName;
        this.textContent = data.getTextContent();
        this.codeContent = data.getCodeContent();
        this.codeLanguage = data.getCodeLanguage();
        this.status = data.getStatus();
        this.runtime = data.getRuntime();
        this.memory = data.getMemory();

        if (data.getComplexityAnalysis() != null) {
            this.complexityAnalysis = new ComplexityAnalysisDTO(
                    data.getComplexityAnalysis().getTimeComplexity(),
                    data.getComplexityAnalysis().getSpaceComplexity(),
                    data.getComplexityAnalysis().getComplexityDescription());
        }

        if (data.getWrongTestcase() != null) {
            this.wrongTestcase = new TestcaseFailureDTO(
                    data.getWrongTestcase().getInput(),
                    data.getWrongTestcase().getUserOutput(),
                    data.getWrongTestcase().getExpectedOutput());
        }

        if (data.getTleTestcase() != null) {
            this.tleTestcase = new TestcaseFailureDTO(
                    data.getTleTestcase().getInput(),
                    data.getTleTestcase().getUserOutput(),
                    data.getTleTestcase().getExpectedOutput());
        }

        this.contentSize = data.getContentSize();
        this.contentSizeKB = data.getContentSize() / 1024.0;
        this.createdAt = data.getCreatedAt();
        this.updatedAt = data.getUpdatedAt();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getCodeContent() {
        return codeContent;
    }

    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }

    public String getCodeLanguage() {
        return codeLanguage;
    }

    public void setCodeLanguage(String codeLanguage) {
        this.codeLanguage = codeLanguage;
    }

    public ApproachStatus getStatus() {
        return status;
    }

    public void setStatus(ApproachStatus status) {
        this.status = status;
    }

    public Long getRuntime() {
        return runtime;
    }

    public void setRuntime(Long runtime) {
        this.runtime = runtime;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public ComplexityAnalysisDTO getComplexityAnalysis() {
        return complexityAnalysis;
    }

    public void setComplexityAnalysis(ComplexityAnalysisDTO complexityAnalysis) {
        this.complexityAnalysis = complexityAnalysis;
    }

    public TestcaseFailureDTO getWrongTestcase() {
        return wrongTestcase;
    }

    public void setWrongTestcase(TestcaseFailureDTO wrongTestcase) {
        this.wrongTestcase = wrongTestcase;
    }

    public TestcaseFailureDTO getTleTestcase() {
        return tleTestcase;
    }

    public void setTleTestcase(TestcaseFailureDTO tleTestcase) {
        this.tleTestcase = tleTestcase;
    }

    public int getContentSize() {
        return contentSize;
    }

    public void setContentSize(int contentSize) {
        this.contentSize = contentSize;
    }

    public double getContentSizeKB() {
        return contentSizeKB;
    }

    public void setContentSizeKB(double contentSizeKB) {
        this.contentSizeKB = contentSizeKB;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}