// src/main/java/com/algoarena/model/CourseTopic.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "course_topics")
public class CourseTopic {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;
    
    private String description;
    private Integer displayOrder;
    private String iconUrl;
    
    private Boolean isPublic = true;
    
    // ✅ NEW: YouTube video links (max 50)
    private List<String> videoLinks = new ArrayList<>();
    private static final int MAX_VIDEO_LINKS = 50;
    
    private String createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;

    public CourseTopic() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isPublic = true;
        this.videoLinks = new ArrayList<>();
    }

    public CourseTopic(String name, String description, User createdBy) {
        this();
        this.name = name;
        this.description = description;
        if (createdBy != null) {
            this.createdById = createdBy.getId();
            this.createdByName = createdBy.getName();
        }
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getVideoLinks() {
        return videoLinks;
    }

    public void setVideoLinks(List<String> videoLinks) {
        this.videoLinks = videoLinks;
        this.updatedAt = LocalDateTime.now();
    }

    public static int getMaxVideoLinks() {
        return MAX_VIDEO_LINKS;
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "CourseTopic{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", isPublic=" + isPublic +
                ", displayOrder=" + displayOrder +
                ", videoLinksCount=" + (videoLinks != null ? videoLinks.size() : 0) +
                '}';
    }
}