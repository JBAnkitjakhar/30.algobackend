// src/main/java/com/algoarena/dto/dsa/AdminQuestionSummaryDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.QuestionLevel;
import java.time.LocalDateTime;
import java.util.List;

public class AdminQuestionSummaryDTO {
    
    private String id;
    private String title;
    private QuestionLevel level;
    private String categoryId; // ONLY ID
    private Integer displayOrder;
    private int imageCount;
    
    // NEW: Code template availability (languages available)
    private List<String> userStarterCodeLanguages;
    private List<String> generalTemplateLanguages;
    private List<String> correctSolutionLanguages;
    
    // NEW: Testcase count
    private int testcaseCount;
    
    private String createdByName;
    private LocalDateTime updatedAt;
    private int solutionCount;
    
    public AdminQuestionSummaryDTO() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public QuestionLevel getLevel() { return level; }
    public void setLevel(QuestionLevel level) { this.level = level; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }
    
    public List<String> getUserStarterCodeLanguages() { return userStarterCodeLanguages; }
    public void setUserStarterCodeLanguages(List<String> userStarterCodeLanguages) { 
        this.userStarterCodeLanguages = userStarterCodeLanguages; 
    }
    
    public List<String> getGeneralTemplateLanguages() { return generalTemplateLanguages; }
    public void setGeneralTemplateLanguages(List<String> generalTemplateLanguages) { 
        this.generalTemplateLanguages = generalTemplateLanguages; 
    }
    
    public List<String> getCorrectSolutionLanguages() { return correctSolutionLanguages; }
    public void setCorrectSolutionLanguages(List<String> correctSolutionLanguages) { 
        this.correctSolutionLanguages = correctSolutionLanguages; 
    }
    
    public int getTestcaseCount() { return testcaseCount; }
    public void setTestcaseCount(int testcaseCount) { this.testcaseCount = testcaseCount; }
    
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public int getSolutionCount() { return solutionCount; }
    public void setSolutionCount(int solutionCount) { this.solutionCount = solutionCount; }
}