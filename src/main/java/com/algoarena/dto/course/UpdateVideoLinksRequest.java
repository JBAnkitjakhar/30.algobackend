// src/main/java/com/algoarena/dto/course/UpdateVideoLinksRequest.java
package com.algoarena.dto.course;

import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdateVideoLinksRequest {
    
    @Size(max = 50, message = "Maximum 50 video links allowed")
    private List<String> videoLinks;

    public UpdateVideoLinksRequest() {}

    public UpdateVideoLinksRequest(List<String> videoLinks) {
        this.videoLinks = videoLinks;
    }

    public List<String> getVideoLinks() {
        return videoLinks;
    }

    public void setVideoLinks(List<String> videoLinks) {
        this.videoLinks = videoLinks;
    }
}