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
            // Remove isExample and explanation from all testcases
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
}