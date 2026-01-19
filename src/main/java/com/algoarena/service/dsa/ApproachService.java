// src/main/java/com/algoarena/service/dsa/ApproachService.java
package com.algoarena.service.dsa;

import com.algoarena.dto.complexity.ComplexityAnalysisRequest;
import com.algoarena.dto.complexity.ComplexityAnalysisResponse;
import com.algoarena.dto.dsa.ApproachDetailDTO;
import com.algoarena.dto.dsa.ApproachMetadataDTO;
import com.algoarena.dto.dsa.ApproachUpdateDTO;
import com.algoarena.model.User;
import com.algoarena.model.UserApproaches;
import com.algoarena.model.UserApproaches.ApproachData;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.repository.UserApproachesRepository;
import com.algoarena.service.complexity.ComplexityAnalysisService;

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

    @Autowired
    private SubmissionTrackingService submissionTrackingService;

    @Autowired
    private ComplexityAnalysisService complexityAnalysisService;

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

        // ✅ Sort by updatedAt descending (newest first)
        return approaches.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
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
        usage.put("percentageUsed",
                String.format("%.1f%%", (usedBytes * 100.0) / UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES));

        return usage;
    }

    /**
     * ✅ Create new approach - ATOMIC with Map-of-Maps
     * ⭐ Now includes submission tracking for heatmap
     */
    public ApproachDetailDTO createApproach(String userId, String questionId, ApproachDetailDTO dto, User currentUser) {
        if (!questionRepository.existsById(questionId)) {
            throw new RuntimeException("Question not found with id: " + questionId);
        }

        ApproachData approach = new ApproachData(questionId, dto.getTextContent());
        approach.setCodeContent(dto.getCodeContent());
        approach.setCodeLanguage(dto.getCodeLanguage() != null ? dto.getCodeLanguage() : "java");
        approach.setStatus(dto.getStatus() != null ? dto.getStatus() : UserApproaches.ApproachStatus.ACCEPTED);

        if (dto.getRuntime() != null)
            approach.setRuntime(dto.getRuntime());
        if (dto.getMemory() != null)
            approach.setMemory(dto.getMemory());

        if (dto.getWrongTestcase() != null) {
            approach.setWrongTestcase(new ApproachData.TestcaseFailure(
                    dto.getWrongTestcase().getInput(),
                    dto.getWrongTestcase().getUserOutput(),
                    dto.getWrongTestcase().getExpectedOutput()));
        }

        if (dto.getTleTestcase() != null) {
            approach.setTleTestcase(new ApproachData.TestcaseFailure(
                    dto.getTleTestcase().getInput(),
                    dto.getTleTestcase().getUserOutput(),
                    dto.getTleTestcase().getExpectedOutput()));
        }

        approach.updateContentSize();

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

        String approachPath = "approaches." + questionId + "." + approach.getId();

        Update update = new Update()
                .setOnInsert("userId", userId)
                .setOnInsert("userName", currentUser.getName())
                .set(approachPath, approach)
                .inc("totalApproaches", 1)
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.upsert(query, update, UserApproaches.class);

        // ⭐ Record submission for heatmap tracking (non-blocking means if it fails,
        // approach is still created)
        try {
            submissionTrackingService.recordSubmission(userId);
        } catch (Exception e) {
            logger.error("Failed to record submission for user {}: {}", userId, e.getMessage());
            // Don't throw - approach was created successfully
        }

        return new ApproachDetailDTO(approach, userId, currentUser.getName());
    }

    /**
     * ✅ Update approach text - ATOMIC with Map-of-Maps
     */
    public ApproachDetailDTO updateApproach(String userId, String questionId, String approachId,
            ApproachUpdateDTO dto) {
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

        int oldSize = approach.getContentSize();

        ApproachData tempApproach = new ApproachData();
        tempApproach.setTextContent(dto.getTextContent());
        tempApproach.setCodeContent(approach.getCodeContent());
        int newSize = tempApproach.calculateContentSize();

        int currentTotal = userApproaches.getTotalSizeForQuestion(questionId);
        int adjustedTotal = currentTotal - oldSize + newSize;

        if (adjustedTotal > UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES) {
            double remainingKB = (UserApproaches.MAX_COMBINED_SIZE_PER_QUESTION_BYTES - (currentTotal - oldSize))
                    / 1024.0;
            throw new RuntimeException(
                    String.format(
                            "Update would exceed 20 KB combined limit! You have %.2f KB remaining for this question.",
                            remainingKB));
        }

        Query query = new Query(Criteria.where("userId").is(userId));

        String textPath = "approaches." + questionId + "." + approachId + ".textContent";
        String sizePath = "approaches." + questionId + "." + approachId + ".contentSize";
        String updatedPath = "approaches." + questionId + "." + approachId + ".updatedAt";

        Update update = new Update()
                .set(textPath, dto.getTextContent())
                .set(sizePath, newSize)
                .set(updatedPath, LocalDateTime.now())
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, UserApproaches.class);

        approach.setTextContent(dto.getTextContent());
        approach.setContentSize(newSize);
        approach.setUpdatedAt(LocalDateTime.now());

        return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
    }

    /**
     * ✅ Smart complexity analysis - checks existing, analyzes if needed
     * No parameters needed - fetches approach internally
     */
    public ApproachDetailDTO analyzeComplexity(String userId, String questionId, String approachId) {
        // 1️⃣ Fetch approach
        Query findQuery = new Query(Criteria.where("userId").is(userId));
        UserApproaches userApproaches = mongoTemplate.findOne(findQuery, UserApproaches.class);

        if (userApproaches == null) {
            throw new RuntimeException("User approaches not found");
        }

        ApproachData approach = userApproaches.findApproachById(approachId);
        if (approach == null) {
            throw new RuntimeException("Approach not found with id: " + approachId);
        }

        // 2️⃣ Validate it's ACCEPTED
        if (approach.getStatus() != UserApproaches.ApproachStatus.ACCEPTED) {
            throw new RuntimeException("Complexity analysis is only available for ACCEPTED approaches");
        }

        // 3️⃣ Check if complexity already exists
        if (approach.getComplexityAnalysis() != null) {
            // logger.info("✅ Complexity already exists for approach {}, returning cached result", approachId);
            return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
        }

        // 4️⃣ No complexity exists → Analyze via Gemini
        // logger.info("🔍 Analyzing complexity for approach {} using Gemini...", approachId);

        if (approach.getCodeContent() == null || approach.getCodeContent().trim().isEmpty()) {
            throw new RuntimeException("No code found to analyze");
        }

        ComplexityAnalysisRequest request = new ComplexityAnalysisRequest(
                approach.getCodeContent(),
                approach.getCodeLanguage());

        ComplexityAnalysisResponse geminiResponse;
        try {
            geminiResponse = complexityAnalysisService.analyzeComplexity(request);
        } catch (Exception e) {
            logger.error("❌ Gemini analysis failed: {}", e.getMessage());
            throw new RuntimeException("AI analysis failed: " + e.getMessage());
        }

        // 5️⃣ Save complexity to database
        ApproachData.ComplexityAnalysis complexity = new ApproachData.ComplexityAnalysis(
                geminiResponse.getTimeComplexity(),
                geminiResponse.getSpaceComplexity(),
                geminiResponse.getComplexityDescription());

        Query query = new Query(Criteria.where("userId").is(userId));
        String complexityPath = "approaches." + questionId + "." + approachId + ".complexityAnalysis";
        String updatedPath = "approaches." + questionId + "." + approachId + ".updatedAt";

        Update update = new Update()
                .set(complexityPath, complexity)
                .set(updatedPath, LocalDateTime.now())
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, UserApproaches.class);

        approach.setComplexityAnalysis(complexity);
        approach.setUpdatedAt(LocalDateTime.now());

        // logger.info("✅ Complexity saved successfully for approach {}", approachId);
        return new ApproachDetailDTO(approach, userId, userApproaches.getUserName());
    }

    /**
     * ✅ Delete single approach - ATOMIC with Map-of-Maps
     */
    public void deleteApproach(String userId, String questionId, String approachId) {
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

        Query query = new Query(Criteria.where("userId").is(userId));
        String approachPath = "approaches." + questionId + "." + approachId;

        Update update = new Update()
                .unset(approachPath)
                .inc("totalApproaches", -1)
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, UserApproaches.class);

        // Verify deletion
        UserApproaches afterDelete = mongoTemplate.findOne(findQuery, UserApproaches.class);
        ApproachData stillExists = afterDelete != null ? afterDelete.findApproachById(approachId) : null;

        if (stillExists != null) {
            logger.error("❌ CRITICAL: Approach {} still exists after delete operation!", approachId);
            throw new RuntimeException("Delete operation failed!");
        }
    }

    /**
     * ✅ Delete all approaches for a question (Admin) - ATOMIC OPERATION
     */
    public void deleteAllApproachesForQuestion(String questionId) {
        Query query = new Query(Criteria.where("approaches." + questionId).exists(true));

        Update update = new Update()
                .unset("approaches." + questionId)
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateMulti(query, update, UserApproaches.class);

        // Fix totalApproaches count for affected users
        List<UserApproaches> affectedUsers = userApproachesRepository.findAll();
        for (UserApproaches user : affectedUsers) {
            int actualTotal = user.getAllApproachesFlat().size();
            if (user.getTotalApproaches() != actualTotal) {
                Query userQuery = new Query(Criteria.where("userId").is(user.getUserId()));
                Update countUpdate = new Update().set("totalApproaches", actualTotal);
                mongoTemplate.updateFirst(userQuery, countUpdate, UserApproaches.class);
            }
        }
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

        Update update = new Update()
                .unset("approaches." + questionId)
                .inc("totalApproaches", -approachCount)
                .set("lastUpdated", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, UserApproaches.class);
    }
}