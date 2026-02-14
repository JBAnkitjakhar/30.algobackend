// // src/main/java/com/algoarena/service/migration/QuestionMethodNameMigrationService.java
// package com.algoarena.service.migration;

// import org.bson.Document;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.mongodb.core.query.Criteria;
// import org.springframework.data.mongodb.core.query.Query;
// import org.springframework.data.mongodb.core.query.Update;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.*;

// @Service
// public class QuestionMethodNameMigrationService {

//     private static final Logger logger = LoggerFactory.getLogger(QuestionMethodNameMigrationService.class);

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * Remove methodName field from all questions
//      */
//     public Map<String, Object> removeMethodNameField() {
//         logger.info("🔄 ========================================");
//         logger.info("🔄 Starting methodName Field Removal Migration");
//         logger.info("🔄 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalQuestions = 0;
//         int removedMethodName = 0;
//         int noMethodName = 0;
//         int failedQuestions = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Get all questions as raw documents
//             Query query = new Query();
//             List<Document> allQuestions = mongoTemplate.find(query, Document.class, "questions");
//             totalQuestions = allQuestions.size();

//             logger.info("📊 Found {} questions to check", totalQuestions);

//             for (Document questionDoc : allQuestions) {
//                 String questionId = "unknown";
//                 try {
//                     questionId = questionDoc.getObjectId("_id").toString();
//                     String title = questionDoc.getString("title");
                    
//                     logger.info("🔍 Processing question: {} - {}", questionId, title);

//                     boolean hasMethodName = questionDoc.containsKey("methodName");

//                     if (!hasMethodName) {
//                         logger.info("✓  Question {} has no methodName field, skipping", questionId);
//                         noMethodName++;
//                         continue;
//                     }

//                     // Remove methodName field
//                     Update update = new Update();
//                     update.unset("methodName");
//                     update.set("updatedAt", LocalDateTime.now());
                    
//                     Query updateQuery = new Query(Criteria.where("_id").is(questionDoc.getObjectId("_id")));
//                     mongoTemplate.updateFirst(updateQuery, update, "questions");

//                     removedMethodName++;
//                     logger.info("✅ Removed methodName from question: {} - {}", questionId, title);

//                 } catch (Exception e) {
//                     String error = String.format("Failed to migrate question %s: %s", questionId, e.getMessage());
//                     logger.error("❌ {}", error);
//                     logger.error("Stack trace:", e);
//                     errors.add(error);
//                     failedQuestions++;
//                 }
//             }

//             result.put("success", true);
//             result.put("totalQuestions", totalQuestions);
//             result.put("removedMethodName", removedMethodName);
//             result.put("noMethodName", noMethodName);
//             result.put("failedQuestions", failedQuestions);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d methodName fields removed, %d already without methodName, %d failed out of %d total questions",
//                 removedMethodName, noMethodName, failedQuestions, totalQuestions));

//             logger.info("\n📈 Migration Summary:");
//             logger.info("   Total Questions: {}", totalQuestions);
//             logger.info("   ✅ methodName Removed: {}", removedMethodName);
//             logger.info("   ✓  Already No methodName: {}", noMethodName);
//             logger.info("   ❌ Failed: {}", failedQuestions);
//             if (!errors.isEmpty()) {
//                 logger.info("   Errors:");
//                 errors.forEach(e -> logger.info("      - {}", e));
//             }

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             logger.error("❌ Migration failed: {}", e.getMessage(), e);
//         }

//         return result;
//     }

//     /**
//      * Verify migration completed successfully
//      */
//     public Map<String, Object> verifyMigration() {
//         logger.info("🔍 Verifying methodName field removal...");

//         Map<String, Object> result = new HashMap<>();
//         List<String> issues = new ArrayList<>();

//         try {
//             Query query = new Query();
//             List<Document> allQuestions = mongoTemplate.find(query, Document.class, "questions");
//             int totalQuestions = allQuestions.size();
//             int verifiedQuestions = 0;
//             int questionsWithMethodName = 0;

//             for (Document questionDoc : allQuestions) {
//                 String questionId = questionDoc.getObjectId("_id").toString();
//                 String title = questionDoc.getString("title");

//                 boolean hasMethodName = questionDoc.containsKey("methodName");

//                 if (hasMethodName) {
//                     issues.add(String.format("Question %s (%s) still has 'methodName' field", questionId, title));
//                     questionsWithMethodName++;
//                 } else {
//                     verifiedQuestions++;
//                 }
//             }

//             result.put("success", true);
//             result.put("totalQuestions", totalQuestions);
//             result.put("verifiedQuestions", verifiedQuestions);
//             result.put("questionsWithMethodName", questionsWithMethodName);
//             result.put("issues", issues);
//             result.put("migrationComplete", questionsWithMethodName == 0);
//             result.put("message", questionsWithMethodName == 0 
//                 ? "✅ Migration verified: All questions have methodName field removed"
//                 : String.format("⚠️ Found %d questions still with methodName field", questionsWithMethodName));

//             logger.info("📊 Verification Summary:");
//             logger.info("   Total Questions: {}", totalQuestions);
//             logger.info("   ✅ Verified (no methodName): {}", verifiedQuestions);
//             logger.info("   ⚠️  Still With methodName: {}", questionsWithMethodName);
//             logger.info("   {}", result.get("message"));

//             if (!issues.isEmpty()) {
//                 logger.warn("⚠️  Issues found:");
//                 issues.forEach(issue -> logger.warn("   - {}", issue));
//             }

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Verification failed: " + e.getMessage());
//             logger.error("❌ Verification failed: {}", e.getMessage(), e);
//         }

//         return result;
//     }

//     /**
//      * Get migration statistics
//      */
//     public Map<String, Object> getMigrationStats() {
//         Map<String, Object> stats = new HashMap<>();

//         try {
//             Query query = new Query();
//             List<Document> allQuestions = mongoTemplate.find(query, Document.class, "questions");
            
//             int totalQuestions = allQuestions.size();
//             int withMethodName = 0;
            
//             for (Document questionDoc : allQuestions) {
//                 boolean hasMethodName = questionDoc.containsKey("methodName");
//                 if (hasMethodName) withMethodName++;
//             }

//             stats.put("totalQuestions", totalQuestions);
//             stats.put("withMethodName", withMethodName);
//             stats.put("withoutMethodName", totalQuestions - withMethodName);
//             stats.put("migrationNeeded", withMethodName > 0);
//             stats.put("message", withMethodName == 0 
//                 ? "✅ No migration needed - all questions already without methodName"
//                 : String.format("⚠️ Migration needed - %d questions have methodName field", withMethodName));

//         } catch (Exception e) {
//             stats.put("error", "Failed to get stats: " + e.getMessage());
//         }

//         return stats;
//     }
// }