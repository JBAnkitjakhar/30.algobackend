// src/main/java/com/algoarena/service/course/CourseReadProgressService.java
package com.algoarena.service.course;

import com.algoarena.dto.course.CourseReadStatsDTO;
import com.algoarena.model.CourseReadProgress;
import com.algoarena.repository.CourseDocRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
public class CourseReadProgressService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CourseDocRepository docRepository;

    @Cacheable(value = "courseReadStats", key = "#userId")
    public CourseReadStatsDTO getUserReadStats(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        CourseReadProgress progress = mongoTemplate.findOne(query, CourseReadProgress.class);

        if (progress == null) {
            return new CourseReadStatsDTO(0, new HashMap<>());
        }

        return new CourseReadStatsDTO(
                progress.getReadDocs().size(),
                progress.getReadDocs());
    }

    @CacheEvict(value = "courseReadStats", key = "#userId")
    public void toggleDocReadStatus(String userId, String docId) {
        if (docId == null || docId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID is required");
        }

        if (!docRepository.existsById(docId)) {
            throw new RuntimeException("Document not found with id: " + docId);
        }

        Query query = new Query(Criteria.where("userId").is(userId));
        CourseReadProgress progress = mongoTemplate.findOne(query, CourseReadProgress.class);

        boolean isCurrentlyRead = progress != null && progress.getReadDocs().containsKey(docId);

        if (isCurrentlyRead) {
            Update update = new Update().unset("readDocs." + docId);
            mongoTemplate.updateFirst(query, update, CourseReadProgress.class);
        } else {
            Update update = new Update()
                    .setOnInsert("userId", userId)
                    .set("readDocs." + docId, LocalDateTime.now());

            mongoTemplate.upsert(query, update, CourseReadProgress.class);
        }
    }

    /**
     * Remove a specific document from ALL users' read progress
     */
    @CacheEvict(value = "courseReadStats", allEntries = true)
    public void removeDocFromAllUsers(String docId) {
        Query query = new Query(Criteria.where("readDocs." + docId).exists(true));
        Update update = new Update().unset("readDocs." + docId);
        
        mongoTemplate.updateMulti(query, update, CourseReadProgress.class);
        
        System.out.println("✓ Removed doc " + docId + " from all users' read progress");
    }

    /**
     * Remove multiple documents from ALL users' read progress
     */
    @CacheEvict(value = "courseReadStats", allEntries = true)
    public void removeDocsFromAllUsers(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return;
        }

        for (String docId : docIds) {
            Query query = new Query(Criteria.where("readDocs." + docId).exists(true));
            Update update = new Update().unset("readDocs." + docId);
            mongoTemplate.updateMulti(query, update, CourseReadProgress.class);
        }
        
        System.out.println("✓ Removed " + docIds.size() + " docs from all users' read progress");
    }
}