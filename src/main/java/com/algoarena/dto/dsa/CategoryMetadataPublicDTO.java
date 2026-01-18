// File: src/main/java/com/algoarena/dto/dsa/CategoryMetadataPublicDTO.java
package com.algoarena.dto.dsa;

/**
 * Public version of CategoryMetadataDTO
 * Excludes: createdByName, createdAt, updatedAt
 */
public class CategoryMetadataPublicDTO {
    
    private String id;
    private String name;
    private int easyCount;
    private int mediumCount;
    private int hardCount;
    private int totalQuestions;
    
    public CategoryMetadataPublicDTO() {}
    
    public static CategoryMetadataPublicDTO fromFull(CategoryMetadataDTO full) {
        CategoryMetadataPublicDTO dto = new CategoryMetadataPublicDTO();
        dto.id = full.getId();
        dto.name = full.getName();
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