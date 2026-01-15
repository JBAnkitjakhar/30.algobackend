// src/main/java/com/algoarena/dto/course/CourseTopicNameDTO.java
package com.algoarena.dto.course;

import com.algoarena.model.CourseTopic;

public class CourseTopicNameDTO {
    
    private String id;
    private String name;
    private String iconUrl; // ✅ ADD THIS
    private Boolean isPublic;
    private Integer displayOrder;

    public CourseTopicNameDTO() {}

    public CourseTopicNameDTO(CourseTopic topic) {
        this.id = topic.getId();
        this.name = topic.getName();
        this.iconUrl = topic.getIconUrl(); // ✅ ADD THIS
        this.isPublic = topic.getIsPublic();
        this.displayOrder = topic.getDisplayOrder();
    }

    public static CourseTopicNameDTO fromEntity(CourseTopic topic) {
        return new CourseTopicNameDTO(topic);
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

    // ✅ ADD THIS GETTER/SETTER
    public String getIconUrl() { 
        return iconUrl; 
    }
    
    public void setIconUrl(String iconUrl) { 
        this.iconUrl = iconUrl; 
    }

    public Boolean getIsPublic() { 
        return isPublic; 
    }
    
    public void setIsPublic(Boolean isPublic) { 
        this.isPublic = isPublic; 
    }

    public Integer getDisplayOrder() { 
        return displayOrder; 
    }
    
    public void setDisplayOrder(Integer displayOrder) { 
        this.displayOrder = displayOrder; 
    }
}