// src/main/java/com/algoarena/model/CourseReadProgress.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "course_read_progress")
public class CourseReadProgress {

    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    @Version
    private Long version;
    
    private Map<String, LocalDateTime> readDocs = new HashMap<>();
    
    public CourseReadProgress() {}
    
    public CourseReadProgress(String userId) {
        this.id = userId;
        this.userId = userId;
    }
    
    public void markDocAsRead(String docId) {
        readDocs.put(docId, LocalDateTime.now());
    }
    
    public boolean isDocRead(String docId) {
        return readDocs.containsKey(docId);
    }
    
    public LocalDateTime getReadAt(String docId) {
        return readDocs.get(docId);
    }
    
    public void unmarkDocAsRead(String docId) {
        readDocs.remove(docId);
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { 
        this.userId = userId;
        this.id = userId;
    }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Map<String, LocalDateTime> getReadDocs() { return readDocs; }
    public void setReadDocs(Map<String, LocalDateTime> readDocs) { this.readDocs = readDocs; }
    
    @Override
    public String toString() {
        return "CourseReadProgress{" +
                "userId='" + userId + '\'' +
                ", totalRead=" + readDocs.size() +
                '}';
    }
}