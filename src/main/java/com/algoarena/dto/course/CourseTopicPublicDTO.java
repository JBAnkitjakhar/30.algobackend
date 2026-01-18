// src/main/java/com/algoarena/dto/course/CourseTopicPublicDTO.java
package com.algoarena.dto.course;

import java.util.ArrayList;
import java.util.List;

public class CourseTopicPublicDTO {
    private String id;
    private String name;
    private String iconUrl;
    private List<String> videoLinks = new ArrayList<>();

    public CourseTopicPublicDTO() {}

    public static CourseTopicPublicDTO fromFull(CourseTopicDTO full) {
        CourseTopicPublicDTO dto = new CourseTopicPublicDTO();
        dto.id = full.getId();
        dto.name = full.getName();
        dto.iconUrl = full.getIconUrl();
        dto.videoLinks = full.getVideoLinks() != null ? full.getVideoLinks() : new ArrayList<>();
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public List<String> getVideoLinks() { return videoLinks; }
    public void setVideoLinks(List<String> videoLinks) { this.videoLinks = videoLinks; }
}