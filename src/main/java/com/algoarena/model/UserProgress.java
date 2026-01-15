// src/main/java/com/algoarena/model/UserProgress.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "userprogress")
public class UserProgress {

    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    // ✅ Removed @Version - using atomic operations now
    
    private Map<String, LocalDateTime> solvedQuestions = new HashMap<>();
    
    public UserProgress() {}
    
    public UserProgress(String userId) {
        this.id = userId;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { 
        this.userId = userId;
        this.id = userId;
    }
    
    public Map<String, LocalDateTime> getSolvedQuestions() { return solvedQuestions; }
    public void setSolvedQuestions(Map<String, LocalDateTime> solvedQuestions) { 
        this.solvedQuestions = solvedQuestions;
    }
    
    @Override
    public String toString() {
        return "UserProgress{" +
                "userId='" + userId + '\'' +
                ", totalSolved=" + solvedQuestions.size() +
                '}';
    }
}