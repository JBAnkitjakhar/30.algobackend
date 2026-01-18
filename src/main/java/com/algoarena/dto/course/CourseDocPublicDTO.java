// src/main/java/com/algoarena/dto/course/CourseDocPublicDTO.java
package com.algoarena.dto.course;

import java.time.LocalDateTime;

public class CourseDocPublicDTO {
    private String id;
    private String title;
    private String topicId;
    private LocalDateTime updatedAt;

    public CourseDocPublicDTO() {}

    public static CourseDocPublicDTO fromFull(CourseDocDTO full) {
        CourseDocPublicDTO dto = new CourseDocPublicDTO();
        dto.id = full.getId();
        dto.title = full.getTitle();
        dto.topicId = full.getTopicId();
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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}