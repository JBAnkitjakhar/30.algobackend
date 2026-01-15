// src/main/java/com/algoarena/service/course/CourseReadProgressService.java
package com.algoarena.service.course;

import com.algoarena.dto.course.CourseReadStatsDTO;
import com.algoarena.model.CourseReadProgress;
import com.algoarena.repository.CourseDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class CourseReadProgressService {

    private static final Logger logger = LoggerFactory.getLogger(CourseReadProgressService.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CourseDocRepository docRepository;

    /**
     * Get user's read statistics
     */
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

    /**
     * Toggle document read/unread status using atomic operations
     */
    @CacheEvict(value = "courseReadStats", key = "#userId")
    public void toggleDocReadStatus(String userId, String docId) {
        if (docId == null || docId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID is required");
        }

        // Verify document exists
        if (!docRepository.existsById(docId)) {
            throw new RuntimeException("Document not found with id: " + docId);
        }

        // Check current status
        Query query = new Query(Criteria.where("userId").is(userId));
        CourseReadProgress progress = mongoTemplate.findOne(query, CourseReadProgress.class);

        boolean isCurrentlyRead = progress != null && progress.getReadDocs().containsKey(docId);

        if (isCurrentlyRead) {
            // ✅ UNMARK: Remove from Map atomically
            Update update = new Update().unset("readDocs." + docId);
            mongoTemplate.updateFirst(query, update, CourseReadProgress.class);
            logger.info("✅ User {} unmarked doc {} as read", userId, docId);
        } else {
            // ✅ MARK: Add to Map atomically
            Update update = new Update()
                    .setOnInsert("userId", userId)
                    .set("readDocs." + docId, LocalDateTime.now());

            mongoTemplate.upsert(query, update, CourseReadProgress.class);
            logger.info("✅ User {} marked doc {} as read", userId, docId);
        }
    }
}
