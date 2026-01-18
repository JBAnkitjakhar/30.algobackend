// File: src/main/java/com/algoarena/dto/dsa/SolutionPublicDTO.java
package com.algoarena.dto.dsa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Public version of SolutionDTO
 * Excludes: createdByName, updatedAt
 * Keeps: createdAt (to show solution creation date)
 */
public class SolutionPublicDTO {

    private String id;
    private String questionId;
    private String content;
    private String driveLink;
    private String youtubeLink;
    private List<String> imageUrls;
    private List<String> visualizerFileIds;
    private Map<String, List<String>> codeTemplates;
    private LocalDateTime createdAt;

    public SolutionPublicDTO() {}

    public static SolutionPublicDTO fromFull(SolutionDTO full) {
        SolutionPublicDTO dto = new SolutionPublicDTO();
        dto.id = full.getId();
        dto.questionId = full.getQuestionId();
        dto.content = full.getContent();
        dto.driveLink = full.getDriveLink();
        dto.youtubeLink = full.getYoutubeLink();
        dto.imageUrls = full.getImageUrls();
        dto.visualizerFileIds = full.getVisualizerFileIds();
        dto.codeTemplates = full.getCodeTemplates();
        dto.createdAt = full.getCreatedAt();
        return dto;
    }

    // Helper methods
    public boolean hasValidDriveLink() {
        return driveLink != null && driveLink.trim().length() > 0 && 
               driveLink.contains("drive.google.com");
    }

    public boolean hasValidYoutubeLink() {
        return youtubeLink != null && youtubeLink.trim().length() > 0 && 
               (youtubeLink.contains("youtube.com") || youtubeLink.contains("youtu.be"));
    }

    public String getYoutubeVideoId() {
        if (!hasValidYoutubeLink()) return null;
        try {
            String url = youtubeLink;
            if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
            }
            if (url.contains("watch?v=")) {
                return url.substring(url.indexOf("watch?v=") + 8).split("&")[0];
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getYoutubeEmbedUrl() {
        String videoId = getYoutubeVideoId();
        return videoId != null ? "https://www.youtube.com/embed/" + videoId : null;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDriveLink() { return driveLink; }
    public void setDriveLink(String driveLink) { this.driveLink = driveLink; }

    public String getYoutubeLink() { return youtubeLink; }
    public void setYoutubeLink(String youtubeLink) { this.youtubeLink = youtubeLink; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<String> getVisualizerFileIds() { return visualizerFileIds; }
    public void setVisualizerFileIds(List<String> visualizerFileIds) { this.visualizerFileIds = visualizerFileIds; }

    public Map<String, List<String>> getCodeTemplates() { return codeTemplates; }
    public void setCodeTemplates(Map<String, List<String>> codeTemplates) { this.codeTemplates = codeTemplates; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}