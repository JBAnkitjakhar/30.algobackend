// src/main/java/com/algoarena/service/migration/QuestionMigrationService.java
package com.algoarena.service.migration;

import com.algoarena.model.Question;
import com.algoarena.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionMigrationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Remove isExample and explanation fields from all testcases
     * Also remove functionSignature field if it exists
     * 
     * POST http://localhost:8080/admin/migrations/questions/cleanup-testcase-fields
     */
    public Map<String, Object> cleanupTestcaseFields() {
        try {
            Update update = new Update()
                .unset("testcases.$[].isExample")
                .unset("testcases.$[].explanation")
                .unset("functionSignature");
            
            long modifiedCount = mongoTemplate.updateMulti(
                new Query(), 
                update, 
                Question.class
            ).getModifiedCount();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Cleaned up testcase fields");
            result.put("questionsModified", modifiedCount);
            result.put("fieldsRemoved", List.of("testcases.isExample", "testcases.explanation", "functionSignature"));
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Initialize empty code templates for questions that don't have them
     * 
     * POST http://localhost:8080/admin/migrations/questions/init-code-templates
     */
    public Map<String, Object> initializeCodeTemplates() {
        List<Question> allQuestions = questionRepository.findAll();
        
        int updatedCount = 0;
        int skippedCount = 0;
        
        for (Question question : allQuestions) {
            boolean needsUpdate = false;
            
            if (question.getUserStarterCode() == null) {
                question.setUserStarterCode(new HashMap<>());
                needsUpdate = true;
            }
            
            if (question.getGeneralTemplate() == null) {
                question.setGeneralTemplate(new HashMap<>());
                needsUpdate = true;
            }
            
            if (question.getCorrectSolution() == null) {
                question.setCorrectSolution(new HashMap<>());
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                questionRepository.save(question);
                updatedCount++;
            } else {
                skippedCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("totalQuestions", allQuestions.size());
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("message", "Initialized empty code template maps for " + updatedCount + " questions");
        
        return result;
    }

    /**
 * ⭐ IMPROVED: Initialize default expectedTimeLimit for all testcases
 * Sets default time limits based on question difficulty level
 * 
 * POST http://localhost:8080/admin/migrations/questions/init-time-limits
 */
public Map<String, Object> initializeTimeLimits() {
    List<Question> allQuestions = questionRepository.findAll();
    
    int updatedCount = 0;
    int skippedCount = 0;
    int testcasesUpdated = 0;
    
    for (Question question : allQuestions) {
        boolean needsUpdate = false;
        
        if (question.getTestcases() != null && !question.getTestcases().isEmpty()) {
            // Determine default time limit based on difficulty
            long defaultTimeLimit = getDefaultTimeLimit(question.getLevel());
            
            for (Question.Testcase testcase : question.getTestcases()) {
                // ✅ Update if null OR if already null (force update)
                if (testcase.getExpectedTimeLimit() == null || testcase.getExpectedTimeLimit() == 0) {
                    testcase.setExpectedTimeLimit(defaultTimeLimit);
                    needsUpdate = true;
                    testcasesUpdated++;
                }
            }
        }
        
        if (needsUpdate) {
            try {
                // Force update timestamp to ensure save
                question.setUpdatedAt(java.time.LocalDateTime.now());
                Question saved = questionRepository.save(question);
                
                // Verify it was saved
                if (saved.getTestcases() != null && 
                    !saved.getTestcases().isEmpty() && 
                    saved.getTestcases().get(0).getExpectedTimeLimit() != null) {
                    updatedCount++;
                } else {
                    System.err.println("Failed to save time limits for question: " + question.getId());
                }
            } catch (Exception e) {
                System.err.println("Error saving question " + question.getId() + ": " + e.getMessage());
            }
        } else {
            skippedCount++;
        }
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("totalQuestions", allQuestions.size());
    result.put("questionsUpdated", updatedCount);
    result.put("questionsSkipped", skippedCount);
    result.put("testcasesUpdated", testcasesUpdated);
    result.put("message", "Initialized time limits for " + testcasesUpdated + " testcases across " + updatedCount + " questions");
    result.put("defaults", Map.of(
        "EASY", "1000ms",
        "MEDIUM", "2000ms",
        "HARD", "3000ms"
    ));
    
    return result;
}

/**
 * Get default time limit based on question level
 */
private long getDefaultTimeLimit(com.algoarena.model.QuestionLevel level) {
    if (level == null) {
        return 2000L; // Default 2 seconds
    }
    
    return switch (level) {
        case EASY -> 1000L;    // 1 second for easy
        case MEDIUM -> 2000L;  // 2 seconds for medium
        case HARD -> 3000L;    // 3 seconds for hard
    };
}}