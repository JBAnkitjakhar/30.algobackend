// src/main/java/com/algoarena/model/Solution.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Document(collection = "solutions")
public class Solution {

    @Id
    private String id;

    @Indexed(name = "questionId_idx")
    private String questionId;

    private String content;
    
    // Links
    private String driveLink;
    private String youtubeLink;
    
    // Media
    private List<String> imageUrls;
    private List<String> visualizerFileIds;
    
    // ✅ NEW: Map<Language, List<CodeStrings>> - supports multiple templates per language
    private Map<String, List<String>> codeTemplates;

    private String createdByName;

    @Indexed(name = "createdAt_idx")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Solution() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.codeTemplates = new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { 
        this.questionId = questionId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDriveLink() { return driveLink; }
    public void setDriveLink(String driveLink) { 
        this.driveLink = driveLink;
        this.updatedAt = LocalDateTime.now();
    }

    public String getYoutubeLink() { return youtubeLink; }
    public void setYoutubeLink(String youtubeLink) { 
        this.youtubeLink = youtubeLink;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { 
        this.imageUrls = imageUrls;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getVisualizerFileIds() { return visualizerFileIds; }
    public void setVisualizerFileIds(List<String> visualizerFileIds) { 
        this.visualizerFileIds = visualizerFileIds;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, List<String>> getCodeTemplates() { return codeTemplates; }
    public void setCodeTemplates(Map<String, List<String>> codeTemplates) { 
        this.codeTemplates = codeTemplates;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean hasValidDriveLink() {
        return driveLink != null && driveLink.trim().length() > 0 && 
               driveLink.contains("drive.google.com");
    }

    public boolean hasValidYoutubeLink() {
        return youtubeLink != null && youtubeLink.trim().length() > 0 && 
               (youtubeLink.contains("youtube.com") || youtubeLink.contains("youtu.be"));
    }

    // ✅ Helper to count total code templates
    public int getTotalCodeTemplatesCount() {
        if (codeTemplates == null || codeTemplates.isEmpty()) {
            return 0;
        }
        return codeTemplates.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    @Override
    public String toString() {
        return "Solution{" +
                "id='" + id + '\'' +
                ", questionId='" + questionId + '\'' +
                ", createdByName='" + createdByName + '\'' +
                ", hasYoutubeLink=" + hasValidYoutubeLink() +
                ", hasDriveLink=" + hasValidDriveLink() +
                ", totalCodeTemplates=" + getTotalCodeTemplatesCount() +
                '}';
    }
}