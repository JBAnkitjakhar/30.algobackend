// src/main/java/com/algoarena/dto/dsa/ApproachMetadataDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.UserApproaches.ApproachData;
import com.algoarena.model.UserApproaches.ApproachStatus;
import java.time.LocalDateTime;

public class ApproachMetadataDTO {

    private String id;
    private String questionId;
    private String userId;
    private String userName;
    
    private String codeLanguage;
    private ApproachStatus status;
    private Long runtime;
    private Long memory;
    private ComplexityAnalysisDTO complexityAnalysis;
    private int contentSize;
    private double contentSizeKB;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ApproachMetadataDTO() {}

    public ApproachMetadataDTO(ApproachData data, String userId, String userName) {
        this.id = data.getId();
        this.questionId = data.getQuestionId();
        this.userId = userId;
        this.userName = userName;
        this.codeLanguage = data.getCodeLanguage();
        this.status = data.getStatus();
        this.runtime = data.getRuntime();
        this.memory = data.getMemory();
        
        if (data.getComplexityAnalysis() != null) {
            this.complexityAnalysis = new ComplexityAnalysisDTO(
                data.getComplexityAnalysis().getTimeComplexity(),
                data.getComplexityAnalysis().getSpaceComplexity()
            );
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
 