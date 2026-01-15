// src/main/java/com/algoarena/service/dsa/UserProgressService.java
package com.algoarena.service.dsa;

import com.algoarena.dto.user.UserMeStatsDTO;
import com.algoarena.dto.user.QuestionSolveStatusDTO;
import com.algoarena.exception.*;
import com.algoarena.repository.QuestionRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.algoarena.model.UserProgress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
public class UserProgressService {

    // private static final Logger logger = LoggerFactory.getLogger(UserProgressService.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Validate question ID
     */
    private void validateQuestionId(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question ID is required");
        }
    }

    /**
     * Get user stats - cached by userId
     */
    @Cacheable(value = "userMeStats", key = "#userId")
    public UserMeStatsDTO getUserMeStats(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        UserProgress progress = mongoTemplate.findOne(query, UserProgress.class);

        if (progress == null) {
            return new UserMeStatsDTO(0, new HashMap<>());
        }

        return new UserMeStatsDTO(
            progress.getSolvedQuestions().size(),
            progress.getSolvedQuestions()
        );
    }

    /**
     * Get question solve status with timestamp
     */
    public QuestionSolveStatusDTO getQuestionSolveStatus(String userId, String questionId) {
        validateQuestionId(questionId);
        
        Query query = new Query(Criteria.where("userId").is(userId));
        UserProgress progress = mongoTemplate.findOne(query, UserProgress.class);
        
        if (progress == null || !progress.getSolvedQuestions().containsKey(questionId)) {
            return QuestionSolveStatusDTO.notSolved();
        }
        
        LocalDateTime solvedAt = progress.getSolvedQuestions().get(questionId);
        return QuestionSolveStatusDTO.solved(solvedAt);
    }

    /**
     * Check if question is solved
     */
    public boolean isQuestionSolved(String userId, String questionId) {
        validateQuestionId(questionId);
        
        Query query = new Query(Criteria.where("userId").is(userId));
        UserProgress progress = mongoTemplate.findOne(query, UserProgress.class);
        
        return progress != null && progress.getSolvedQuestions().containsKey(questionId);
    }

    /**
     * Mark question as solved using atomic operations
     */
    @CacheEvict(value = "userMeStats", key = "#userId")
    public void markQuestionAsSolved(String userId, String questionId) {
        validateQuestionId(questionId);

        // Verify question exists
        if (!questionRepository.existsById(questionId)) {
            throw new QuestionNotFoundException(questionId);
        }

        // Check if already solved
        Query query = new Query(Criteria.where("userId").is(userId));
        UserProgress progress = mongoTemplate.findOne(query, UserProgress.class);

        if (progress != null && progress.getSolvedQuestions().containsKey(questionId)) {
            throw new QuestionAlreadySolvedException(questionId);
        }

        // ✅ MARK: Add to Map atomically
        Update update = new Update()
            .setOnInsert("userId", userId)
            .set("solvedQuestions." + questionId, LocalDateTime.now());
        
        mongoTemplate.upsert(query, update, UserProgress.class);
        // logger.info("✅ User {} marked question {} as solved", userId, questionId);
    }

    /**
     * Unmark question using atomic operations
     */
    @CacheEvict(value = "userMeStats", key = "#userId")
    public void unmarkQuestionAsSolved(String userId, String questionId) {
        validateQuestionId(questionId);

        // Check if question is solved
        Query query = new Query(Criteria.where("userId").is(userId));
        UserProgress progress = mongoTemplate.findOne(query, UserProgress.class);

        if (progress == null || !progress.getSolvedQuestions().containsKey(questionId)) {
            throw new QuestionNotSolvedException(questionId);
        }

        // ✅ UNMARK: Remove from Map atomically
        Update update = new Update().unset("solvedQuestions." + questionId);
        mongoTemplate.updateFirst(query, update, UserProgress.class);
        
        // logger.info("✅ User {} unmarked question {}", userId, questionId);
    }

    /**
     * Remove question from all users (Admin operation)
     */
    @CacheEvict(value = "userMeStats", allEntries = true)
    public int removeQuestionFromAllUsers(String questionId) {
        Query query = new Query(Criteria.where("solvedQuestions." + questionId).exists(true));
        Update update = new Update().unset("solvedQuestions." + questionId);
        
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateMulti(
            query, 
            update, 
            UserProgress.class
        );

        int removedCount = (int) result.getModifiedCount();
        // logger.info("Removed question {} from {} users", questionId, removedCount);
        return removedCount;
    }

    /**
     * Remove questions from all users (Admin operation)
     */
    @CacheEvict(value = "userMeStats", allEntries = true)
    public int removeQuestionsFromAllUsers(List<String> questionIds) {
        int totalRemoved = 0;

        for (String questionId : questionIds) {
            totalRemoved += removeQuestionFromAllUsers(questionId);
        }

        // logger.info("Removed {} question entries from all users", totalRemoved);
        return totalRemoved;
    }
}