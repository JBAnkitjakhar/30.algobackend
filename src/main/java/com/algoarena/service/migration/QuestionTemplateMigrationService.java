// // src/main/java/com/algoarena/service/migration/QuestionTemplateMigrationService.java
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
// public class QuestionTemplateMigrationService {

//     private static final Logger logger = LoggerFactory.getLogger(QuestionTemplateMigrationService.class);

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * ✅ Rename generalTemplate → submitTemplate
//      * ✅ Rename correctSolution → runTemplate
//      */
//     public Map<String, Object> migrateTemplateFields() {
//         logger.info("🔄 ========================================");
//         logger.info("🔄 Starting Template Field Rename Migration");
//         logger.info("🔄 generalTemplate → submitTemplate");
//         logger.info("🔄 correctSolution → runTemplate");
//         logger.info("🔄 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalQuestions = 0;
//         int migratedGeneralTemplate = 0;
//         int migratedCorrectSolution = 0;
//         int alreadyMigrated = 0;
//         int noTemplates = 0;
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

//                     boolean hasGeneralTemplate = questionDoc.containsKey("generalTemplate");
//                     boolean hasCorrectSolution = questionDoc.containsKey("correctSolution");
//                     boolean hasSubmitTemplate = questionDoc.containsKey("submitTemplate");
//                     boolean hasRunTemplate = questionDoc.containsKey("runTemplate");

//                     // Check if already migrated
//                     if ((hasSubmitTemplate || hasRunTemplate) && !hasGeneralTemplate && !hasCorrectSolution) {
//                         logger.info("✓  Question {} already migrated, skipping", questionId);
//                         alreadyMigrated++;
//                         continue;
//                     }

//                     // Check if has no templates at all
//                     if (!hasGeneralTemplate && !hasCorrectSolution && !hasSubmitTemplate && !hasRunTemplate) {
//                         logger.info("⏭️  Question {} has no templates, skipping", questionId);
//                         noTemplates++;
//                         continue;
//                     }

//                     // Perform migration
//                     Update update = new Update();
//                     boolean needsUpdate = false;

//                     // Migrate generalTemplate → submitTemplate
//                     if (hasGeneralTemplate) {
//                         Object generalTemplateValue = questionDoc.get("generalTemplate");
//                         update.set("submitTemplate", generalTemplateValue);
//                         update.unset("generalTemplate");
//                         needsUpdate = true;
//                         migratedGeneralTemplate++;
//                         logger.info("   📝 Renaming generalTemplate → submitTemplate");
//                     }

//                     // Migrate correctSolution → runTemplate
//                     if (hasCorrectSolution) {
//                         Object correctSolutionValue = questionDoc.get("correctSolution");
//                         update.set("runTemplate", correctSolutionValue);
//                         update.unset("correctSolution");
//                         needsUpdate = true;
//                         migratedCorrectSolution++;
//                         logger.info("   📝 Renaming correctSolution → runTemplate");
//                     }

//                     if (needsUpdate) {
//                         update.set("updatedAt", LocalDateTime.now());
                        
//                         Query updateQuery = new Query(Criteria.where("_id").is(questionDoc.getObjectId("_id")));
//                         mongoTemplate.updateFirst(updateQuery, update, "questions");

//                         logger.info("✅ Migrated question: {} - {}", questionId, title);
//                     }

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
//             result.put("migratedGeneralTemplate", migratedGeneralTemplate);
//             result.put("migratedCorrectSolution", migratedCorrectSolution);
//             result.put("alreadyMigrated", alreadyMigrated);
//             result.put("noTemplates", noTemplates);
//             result.put("failedQuestions", failedQuestions);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d generalTemplate renamed, %d correctSolution renamed, %d already migrated, %d no templates, %d failed out of %d total questions",
//                 migratedGeneralTemplate, migratedCorrectSolution, alreadyMigrated, noTemplates, failedQuestions, totalQuestions));

//             logger.info("\n📈 Migration Summary:");
//             logger.info("   Total Questions: {}", totalQuestions);
//             logger.info("   ✅ generalTemplate → submitTemplate: {}", migratedGeneralTemplate);
//             logger.info("   ✅ correctSolution → runTemplate: {}", migratedCorrectSolution);
//             logger.info("   ✓  Already Migrated: {}", alreadyMigrated);
//             logger.info("   ⏭️  No Templates: {}", noTemplates);
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
//      * ✅ Verify migration completed successfully
//      */
//     public Map<String, Object> verifyMigration() {
//         logger.info("🔍 Verifying template field migration...");

//         Map<String, Object> result = new HashMap<>();
//         List<String> issues = new ArrayList<>();

//         try {
//             Query query = new Query();
//             List<Document> allQuestions = mongoTemplate.find(query, Document.class, "questions");
//             int totalQuestions = allQuestions.size();
//             int verifiedQuestions = 0;
//             int questionsWithOldFields = 0;
//             int questionsWithNewFields = 0;
//             int questionsWithNoTemplates = 0;

//             for (Document questionDoc : allQuestions) {
//                 String questionId = questionDoc.getObjectId("_id").toString();
//                 String title = questionDoc.getString("title");

//                 boolean hasGeneralTemplate = questionDoc.containsKey("generalTemplate");
//                 boolean hasCorrectSolution = questionDoc.containsKey("correctSolution");
//                 boolean hasSubmitTemplate = questionDoc.containsKey("submitTemplate");
//                 boolean hasRunTemplate = questionDoc.containsKey("runTemplate");

//                 // Check for old field names
//                 if (hasGeneralTemplate) {
//                     issues.add(String.format("Question %s (%s) still has 'generalTemplate' field", questionId, title));
//                     questionsWithOldFields++;
//                 }
//                 if (hasCorrectSolution) {
//                     issues.add(String.format("Question %s (%s) still has 'correctSolution' field", questionId, title));
//                     questionsWithOldFields++;
//                 }

//                 // Count new fields
//                 if (hasSubmitTemplate || hasRunTemplate) {
//                     questionsWithNewFields++;
//                 }

//                 // Count no templates
//                 if (!hasGeneralTemplate && !hasCorrectSolution && !hasSubmitTemplate && !hasRunTemplate) {
//                     questionsWithNoTemplates++;
//                 }

//                 if (!hasGeneralTemplate && !hasCorrectSolution) {
//                     verifiedQuestions++;
//                 }
//             }

//             result.put("success", true);
//             result.put("totalQuestions", totalQuestions);
//             result.put("verifiedQuestions", verifiedQuestions);
//             result.put("questionsWithOldFields", questionsWithOldFields);
//             result.put("questionsWithNewFields", questionsWithNewFields);
//             result.put("questionsWithNoTemplates", questionsWithNoTemplates);
//             result.put("issues", issues);
//             result.put("migrationComplete", questionsWithOldFields == 0);
//             result.put("message", questionsWithOldFields == 0 
//                 ? "✅ Migration verified: All questions use new field names"
//                 : String.format("⚠️ Found %d questions with old field names", questionsWithOldFields));

//             logger.info("📊 Verification Summary:");
//             logger.info("   Total Questions: {}", totalQuestions);
//             logger.info("   ✅ Verified (no old fields): {}", verifiedQuestions);
//             logger.info("   ⚠️  With Old Fields: {}", questionsWithOldFields);
//             logger.info("   ✓  With New Fields: {}", questionsWithNewFields);
//             logger.info("   ⏭️  No Templates: {}", questionsWithNoTemplates);
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
//      * ✅ Get migration statistics
//      */
//     public Map<String, Object> getMigrationStats() {
//         Map<String, Object> stats = new HashMap<>();

//         try {
//             Query query = new Query();
//             List<Document> allQuestions = mongoTemplate.find(query, Document.class, "questions");
            
//             int totalQuestions = allQuestions.size();
//             int withGeneralTemplate = 0;
//             int withCorrectSolution = 0;
//             int withSubmitTemplate = 0;
//             int withRunTemplate = 0;
//             int withNoTemplates = 0;
            
//             for (Document questionDoc : allQuestions) {
//                 boolean hasGeneralTemplate = questionDoc.containsKey("generalTemplate");
//                 boolean hasCorrectSolution = questionDoc.containsKey("correctSolution");
//                 boolean hasSubmitTemplate = questionDoc.containsKey("submitTemplate");
//                 boolean hasRunTemplate = questionDoc.containsKey("runTemplate");

//                 if (hasGeneralTemplate) withGeneralTemplate++;
//                 if (hasCorrectSolution) withCorrectSolution++;
//                 if (hasSubmitTemplate) withSubmitTemplate++;
//                 if (hasRunTemplate) withRunTemplate++;
                
//                 if (!hasGeneralTemplate && !hasCorrectSolution && !hasSubmitTemplate && !hasRunTemplate) {
//                     withNoTemplates++;
//                 }
//             }

//             stats.put("totalQuestions", totalQuestions);
//             stats.put("withGeneralTemplate", withGeneralTemplate);
//             stats.put("withCorrectSolution", withCorrectSolution);
//             stats.put("withSubmitTemplate", withSubmitTemplate);
//             stats.put("withRunTemplate", withRunTemplate);
//             stats.put("withNoTemplates", withNoTemplates);
//             stats.put("migrationNeeded", withGeneralTemplate > 0 || withCorrectSolution > 0);

//         } catch (Exception e) {
//             stats.put("error", "Failed to get stats: " + e.getMessage());
//         }

//         return stats;
//     }
// }