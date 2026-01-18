// File: src/main/java/com/algoarena/dto/dsa/CategoryPublicDTO.java
package com.algoarena.dto.dsa;

import java.util.ArrayList;
import java.util.List;

/**
 * Public version of CategoryDTO
 * Excludes: displayOrder, createdByName, createdById, createdAt, updatedAt
 */
public class CategoryPublicDTO {
    
    private String id;
    private String name;
    
    private List<String> easyQuestionIds = new ArrayList<>();
    private List<String> mediumQuestionIds = new ArrayList<>();
    private List<String> hardQuestionIds = new ArrayList<>();
    
    private int easyCount;
    private int mediumCount;
    private int hardCount;
    private int totalQuestions;
    
    public CategoryPublicDTO() {}
    
    public static CategoryPublicDTO fromFull(CategoryDTO full) {
        CategoryPublicDTO dto = new CategoryPublicDTO();
        dto.id = full.getId();
        dto.name = full.getName();
        dto.easyQuestionIds = new ArrayList<>(full.getEasyQuestionIds());
        dto.mediumQuestionIds = new ArrayList<>(full.getMediumQuestionIds());
        dto.hardQuestionIds = new ArrayList<>(full.getHardQuestionIds());
        dto.easyCount = full.getEasyCount();
        dto.mediumCount = full.getMediumCount();
        dto.hardCount = full.getHardCount();
        dto.totalQuestions = full.getTotalQuestions();
        return dto;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getEasyQuestionIds() {
        return easyQuestionIds;
    }
    
    public void setEasyQuestionIds(List<String> easyQuestionIds) {
        this.easyQuestionIds = easyQuestionIds;
    }
    
    public List<String> getMediumQuestionIds() {
        return mediumQuestionIds;
    }
    
    public void setMediumQuestionIds(List<String> mediumQuestionIds) {
        this.mediumQuestionIds = mediumQuestionIds;
    }
    
    public List<String> getHardQuestionIds() {
        return hardQuestionIds;
    }
    
    public void setHardQuestionIds(List<String> hardQuestionIds) {
        this.hardQuestionIds = hardQuestionIds;
    }
    
    public int getEasyCount() {
        return easyCount;
    }
    
    public void setEasyCount(int easyCount) {
        this.easyCount = easyCount;
    }
    
    public int getMediumCount() {
        return mediumCount;
    }
    
    public void setMediumCount(int mediumCount) {
        this.mediumCount = mediumCount;
    }
    
    public int getHardCount() {
        return hardCount;
    }
    
    public void setHardCount(int hardCount) {
        this.hardCount = hardCount;
    }
    
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
}