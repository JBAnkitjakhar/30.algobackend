// src/main/java/com/algoarena/dto/course/CourseReadStatsDTO.java
package com.algoarena.dto.course;

import java.time.LocalDateTime;
import java.util.Map;

public class CourseReadStatsDTO {
    
    private int totalRead;
    private Map<String, LocalDateTime> readDocs;
    
    public CourseReadStatsDTO(int totalRead, Map<String, LocalDateTime> readDocs) {
        this.totalRead = totalRead;
        this.readDocs = readDocs;
    }
    
    public int getTotalRead() { return totalRead; }
    public void setTotalRead(int totalRead) { this.totalRead = totalRead; }
    
    public Map<String, LocalDateTime> getReadDocs() { return readDocs; }
    public void setReadDocs(Map<String, LocalDateTime> readDocs) { this.readDocs = readDocs; }
}