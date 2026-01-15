// src/main/java/com/algoarena/service/dsa/ApproachService.java
package com.algoarena.service.dsa;

import com.algoarena.dto.dsa.ApproachDetailDTO;
import com.algoarena.dto.dsa.ApproachMetadataDTO;
import com.algoarena.dto.dsa.ApproachUpdateDTO;
import com.algoarena.dto.dsa.ComplexityAnalysisDTO;
import com.algoarena.model.User;
import com.algoarena.model.UserApproaches;
import com.algoarena.model.UserApproaches.ApproachData;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.repository.UserApproachesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApproachService {

    private static final Logger logger = LoggerFactory.getLogger(ApproachService.class);

    @Autowired
    private UserApproachesRepository userApproachesRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Get all approaches metadata for a question (list view - no full content)
     */
    public List<ApproachMetadataDTO> getMyApproachesForQuestion(String userId, String questionId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(query, UserApproaches.class);

        if (userApproaches == null) {
            return new ArrayList<>();
        }

        List<ApproachData> approaches = userApproaches.getApproachesForQuestion(questionId);
        return approaches.stream()
                .map(data -> new ApproachMetadataDTO(data, userId, userApproaches.getUserName()))
                .collect(Collectors.toList());
    }

    /**
     * Get single approach with full content
     */
    public ApproachDetailDTO getMyApproachDetail(String userId, String questionId, String approachId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(query, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        ApproachData approach = userApproaches.findApproachById(approachId);
        if (approach == null) {
            throw new RuntimeException("Approach not found with id: " + approachId);
        }

        return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
    }

    /**
     * Get space usage for a question
     */
    public Map<String, Object> getMyQuestionUsage(String userId, String questionId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(query, UserApproaches.class);

        int usedBytes = 0;
        int approachCount = 0;

        if (userApproaches != null) {
            usedBytes = userApproaches.getTotalSizeForQuestion(questionId);
            approachCount = userApproaches.getApproachCountForQuestion(questionId);
        }

        int remainingBytes = UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES - usedBytes;
        double usedKB = usedBytes / 1024.0;
        double remainingKB = remainingBytes / 1024.0;
        double maxKB = UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES / 1024.0;

        Map<String, Object> usage = new HashMap<>();
        usage.put("usedBytes", usedBytes);
        usage.put("remainingBytes", remainingBytes);
        usage.put("usedKB", String.format("%.2f KB", usedKB));
        usage.put("remainingKB", String.format("%.2f KB", remainingKB));
        usage.put("maxKB", String.format("%.2f KB", maxKB));
        usage.put("approachCount", approachCount);
        usage.put("percentageUsed", String.format("%.1f%%", (usedBytes * 100.0) / UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES));

        return usage;
    }

    /**
     * ✅ Create new approach - ATOMIC OPERATION
     */
    public ApproachDetailDTO createApproach(String userId, String questionId, ApproachDetailDTO dto, User currentUser) {
        // Validate question exists
        if (!questionRepository.existsById(questionId)) {
            throw new RuntimeException("Question not found with id: " + questionId);
        }

        // Create new approach from DTO
        ApproachData approach = new ApproachData(questionId, dto.getTextContent());
        approach.setCodeContent(dto.getCodeContent());
        approach.setCodeLanguage(dto.getCodeLanguage() != null ? dto.getCodeLanguage() : "java");
        approach.setStatus(dto.getStatus() != null ? dto.getStatus() : UserApproaches.ApproachStatus.ACCEPTED);
        
        if (dto.getRuntime() != null) approach.setRuntime(dto.getRuntime());
        if (dto.getMemory() != null) approach.setMemory(dto.getMemory());
        
        // Convert DTO testcase failures to model
        if (dto.getWrongTestcase() != null) {
            approach.setWrongTestcase(new ApproachData.TestcaseFailure(
                dto.getWrongTestcase().getInput(),
                dto.getWrongTestcase().getUserOutput(),
                dto.getWrongTestcase().getExpectedOutput()
            ));
        }
        
        if (dto.getTleTestcase() != null) {
            approach.setTleTestcase(new ApproachData.TestcaseFailure(
                dto.getTleTestcase().getInput(),
                dto.getTleTestcase().getUserOutput(),
                dto.getTleTestcase().getExpectedOutput()
            ));
        }
        
        approach.updateContentSize();

        // Check size limit
        Query query = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(query, UserApproaches.class);
        
        if (userApproaches != null) {
            int currentSize = userApproaches.getTotalSizeForQuestion(questionId);
            int newTotal = currentSize + approach.getContentSize();
            
            if (newTotal > UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES) {
                double remainingKB = (UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES - currentSize) / 1024.0;
                double attemptedKB = approach.getContentSize() / 1024.0;
                throw new RuntimeException(
                    String.format("Combined size limit exceeded! You have %.2f KB remaining for this question, " +
                            "but this approach is %.2f KB. Total limit is 20 KB across all approaches.",
                            remainingKB, attemptedKB));
            }
        }

        // ✅ ATOMIC: Add approach using $push
        Update update = new Update()
            .setOnInsert("userId", userId)
            .setOnInsert("userName", currentUser.getName())
            .push("approaches." + questionId, approach)
            .inc("totalApproaches", 1)
            .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.upsert(query, update, UserApproaches.class);

        logger.info("✅ User {} added approach to question {}", userId, questionId);
        
        return new ApproachDetailDTO(approach, userId, currentUser.getName());
    }

    /**
     * ✅ Update approach text only - ATOMIC OPERATION (FIXED)
     */
    public ApproachDetailDTO updateApproach(String userId, String questionId, String approachId, 
                                           ApproachUpdateDTO dto) {
        // Find the approach first
        Query findQuery = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(findQuery, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        ApproachData approach = userApproaches.findApproachById(approachId);
        if (approach == null) {
            throw new RuntimeException("Approach not found with id: " + approachId);
        }

        if (!approach.getQuestionId().equals(questionId)) {
            throw new RuntimeException("Approach does not belong to this question");
        }

        // Find the index of the approach in the list
        List<ApproachData> questionApproaches = userApproaches.getApproachesForQuestion(questionId);
        int approachIndex = -1;
        for (int i = 0; i < questionApproaches.size(); i++) {
            if (questionApproaches.get(i).getId().equals(approachId)) {
                approachIndex = i;
                break;
            }
        }

        if (approachIndex == -1) {
            throw new RuntimeException("Approach index not found");
        }

        int oldSize = approach.getContentSize();
        
        // Calculate new size
        ApproachData tempApproach = new ApproachData();
        tempApproach.setTextContent(dto.getTextContent());
        tempApproach.setCodeContent(approach.getCodeContent());
        int newSize = tempApproach.calculateContentSize();

        // Check size limit
        int currentTotal = userApproaches.getTotalSizeForQuestion(questionId);
        int adjustedTotal = currentTotal - oldSize + newSize;

        if (adjustedTotal > UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES) {
            double remainingKB = (UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES - (currentTotal - oldSize)) / 1024.0;
            throw new RuntimeException(
                String.format("Update would exceed 20 KB combined limit! You have %.2f KB remaining for this question.",
                        remainingKB));
        }

        // ✅ FIXED: Update using array index notation
        Query query = new Query(Criteria.where("userId").is(userId));
        
        Update update = new Update()
            .set("approaches." + questionId + "." + approachIndex + ".textContent", dto.getTextContent())
            .set("approaches." + questionId + "." + approachIndex + ".contentSize", newSize)
            .set("approaches." + questionId + "." + approachIndex + ".updatedAt", LocalDateTime.now())
            .set("lastUpdated", LocalDateTime.now());

        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, UserApproaches.class);
        
        logger.info("✅ Update operation result: matched={}, modified={}", 
            result.getMatchedCount(), result.getModifiedCount());
        logger.info("✅ User {} updated approach {}", userId, approachId);

        // Return updated approach
        approach.setTextContent(dto.getTextContent());
        approach.setContentSize(newSize);
        approach.setUpdatedAt(LocalDateTime.now());
        
        return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
    }

    /**
     * ✅ Analyze complexity (one-time write) - ATOMIC OPERATION (FIXED)
     */
    public ApproachDetailDTO analyzeComplexity(String userId, String questionId, String approachId, 
                                               ComplexityAnalysisDTO complexityDTO) {
        // Find approach first
        Query findQuery = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(findQuery, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        ApproachData approach = userApproaches.findApproachById(approachId);
        if (approach == null) {
            throw new RuntimeException("Approach not found with id: " + approachId);
        }

        // Validate: Only ACCEPTED approaches
        if (approach.getStatus() != UserApproaches.ApproachStatus.ACCEPTED) {
            throw new RuntimeException("Complexity analysis can only be added to ACCEPTED approaches");
        }

        // Validate: Only if complexity is null (one-time write)
        if (approach.getComplexityAnalysis() != null) {
            throw new RuntimeException("Complexity analysis already exists and cannot be modified");
        }

        // Find the index
        List<ApproachData> questionApproaches = userApproaches.getApproachesForQuestion(questionId);
        int approachIndex = -1;
        for (int i = 0; i < questionApproaches.size(); i++) {
            if (questionApproaches.get(i).getId().equals(approachId)) {
                approachIndex = i;
                break;
            }
        }

        if (approachIndex == -1) {
            throw new RuntimeException("Approach index not found");
        }

        // Convert DTO to model
        ApproachData.ComplexityAnalysis complexity = new ApproachData.ComplexityAnalysis(
            complexityDTO.getTimeComplexity(),
            complexityDTO.getSpaceComplexity(),
            complexityDTO.getComplexityDescription()
        );

        // ✅ FIXED: Update using array index notation
        Query query = new Query(Criteria.where("userId").is(userId));
        
        Update update = new Update()
            .set("approaches." + questionId + "." + approachIndex + ".complexityAnalysis", complexity)
            .set("approaches." + questionId + "." + approachIndex + ".updatedAt", LocalDateTime.now())
            .set("lastUpdated", LocalDateTime.now());

        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, UserApproaches.class);
        
        logger.info("✅ Complexity update result: matched={}, modified={}", 
            result.getMatchedCount(), result.getModifiedCount());
        logger.info("✅ User {} added complexity analysis to approach {}", userId, approachId);

        // Return updated approach
        approach.setComplexityAnalysis(complexity);
        approach.setUpdatedAt(LocalDateTime.now());
        
        return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
    }

    /**
     * ✅ Delete single approach - ATOMIC OPERATION
     */
    public void deleteApproach(String userId, String questionId, String approachId) {
        // Validate approach exists
        Query findQuery = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(findQuery, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        ApproachData approach = userApproaches.findApproachById(approachId);
        if (approach == null) {
            throw new RuntimeException("Approach not found with id: " + approachId);
        }

        if (!approach.getQuestionId().equals(questionId)) {
            throw new RuntimeException("Approach does not belong to this question");
        }

        // ✅ Using $pull with Document
        Query query = new Query(Criteria.where("userId").is(userId));
        
        org.bson.Document pullQuery = new org.bson.Document("id", approachId);
        
        Update update = new Update()
            .pull("approaches." + questionId, pullQuery)
            .inc("totalApproaches", -1)
            .set("lastUpdated", LocalDateTime.now());

        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, UserApproaches.class);
        
        logger.info("✅ Delete operation result: matched={}, modified={}", 
            result.getMatchedCount(), result.getModifiedCount());
        logger.info("✅ User {} deleted approach {} from question {}", userId, approachId, questionId);
    }

    /**
     * ✅ Delete all approaches for a question (Admin) - ATOMIC OPERATION
     */
    public void deleteAllApproachesForQuestion(String questionId) {
        // Find all users who have approaches for this question
        Query query = new Query(Criteria.where("approaches." + questionId).exists(true));
        
        // ✅ ATOMIC: Remove question's approaches from all users
        Update update = new Update()
            .unset("approaches." + questionId)
            .set("lastUpdated", LocalDateTime.now());

        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateMulti(query, update, UserApproaches.class);

        // Update totalApproaches count for affected users
        List<UserApproaches> affectedUsers = userApproachesRepository.findAll();
        for (UserApproaches user : affectedUsers) {
            int actualTotal = user.getAllApproachesFlat().size();
            if (user.getTotalApproaches() != actualTotal) {
                Query userQuery = new Query(Criteria.where("userId").is(user.getUserId()));
                Update countUpdate = new Update().set("totalApproaches", actualTotal);
                mongoTemplate.updateFirst(userQuery, countUpdate, UserApproaches.class);
            }
        }

        logger.info("✅ Deleted all approaches for question {} from {} users", questionId, result.getModifiedCount());
    }

    /**
     * ✅ Delete all approaches by a user for a question (Admin) - ATOMIC OPERATION
     */
    public void deleteAllApproachesByUserForQuestion(String userId, String questionId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(query, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        int approachCount = userApproaches.getApproachCountForQuestion(questionId);

        // ✅ ATOMIC: Remove question approaches
        Update update = new Update()
            .unset("approaches." + questionId)
            .inc("totalApproaches", -approachCount)
            .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, UserApproaches.class);

        logger.info("✅ Deleted {} approaches for question {} from user {}", approachCount, questionId, userId);
    }
}