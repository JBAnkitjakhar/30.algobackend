// src/main/java/com/algoarena/dto/course/CourseDocDetailPublicDTO.java
package com.algoarena.dto.course;

import java.time.LocalDateTime;
import java.util.List;

public class CourseDocDetailPublicDTO {
    private String id;
    private String title;
    private String topicId;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime updatedAt;

    public CourseDocDetailPublicDTO() {}

    public static CourseDocDetailPublicDTO fromFull(CourseDocDTO full) {
        CourseDocDetailPublicDTO dto = new CourseDocDetailPublicDTO();
        dto.id = full.getId();
        dto.title = full.getTitle();
        dto.topicId = full.getTopicId();
        dto.content = full.getContent();
        dto.imageUrls = full.getImageUrls();
        dto.updatedAt = full.getUpdatedAt();
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}