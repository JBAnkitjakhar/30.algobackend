// src/main/java/com/algoarena/service/course/CourseReadProgressService.java
package com.algoarena.service.course;

import com.algoarena.dto.course.CourseReadStatsDTO;
import com.algoarena.exception.ConcurrentModificationException;
import com.algoarena.model.CourseReadProgress;
import com.algoarena.repository.CourseReadProgressRepository;
import com.algoarena.repository.CourseDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class CourseReadProgressService {

    private static final Logger logger = LoggerFactory.getLogger(CourseReadProgressService.class);
    private static final int MAX_RETRIES = 3;

    @Autowired
    private CourseReadProgressRepository readProgressRepository;

    @Autowired
    private CourseDocRepository docRepository;

    @Cacheable(value = "courseReadStats", key = "#userId")
    public CourseReadStatsDTO getUserReadStats(String userId) {
        CourseReadProgress progress = readProgressRepository.findByUserId(userId)
                .orElse(new CourseReadProgress(userId));

        int totalRead = progress.getReadDocs().size();
        
        return new CourseReadStatsDTO(totalRead, progress.getReadDocs());
    }

    private CourseReadProgress getOrCreateReadProgress(String userId) {
        return readProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CourseReadProgress progress = new CourseReadProgress(userId);
                    return readProgressRepository.save(progress);
                });
    }

    @CacheEvict(value = "courseReadStats", key = "#userId")
    public void toggleDocReadStatus(String userId, String docId) {
        if (docId == null || docId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID is required");
        }
        
        int attempt = 0;
        
        while (attempt < MAX_RETRIES) {
            try {
                if (!docRepository.existsById(docId)) {
                    throw new RuntimeException("Document not found with id: " + docId);
                }

                CourseReadProgress progress = getOrCreateReadProgress(userId);

                if (progress.isDocRead(docId)) {
                    progress.unmarkDocAsRead(docId);
                    logger.info("✅ User {} unmarked doc {} as read", userId, docId);
                } else {
                    progress.markDocAsRead(docId);
                    logger.info("✅ User {} marked doc {} as read", userId, docId);
                }
                
                readProgressRepository.save(progress);
                return;
                
            } catch (IllegalArgumentException e) {
                throw e;
                
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    logger.error("❌ Failed to toggle read status after {} attempts", MAX_RETRIES);
                    throw new ConcurrentModificationException();
                }
                
                logger.warn("⚠️ Optimistic lock conflict, retrying... (attempt {}/{})", 
                        attempt, MAX_RETRIES);
                
                try {
                    Thread.sleep(50 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Operation interrupted");
                }
            }
        }
    }
}