// // src/main/java/com/algoarena/service/migration/UserApproachesMigrationService.java
// package com.algoarena.service.migration;

// import com.algoarena.repository.UserApproachesRepository;
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
// public class UserApproachesMigrationService {

//     private static final Logger logger = LoggerFactory.getLogger(UserApproachesMigrationService.class);

//     @Autowired
//     private UserApproachesRepository userApproachesRepository;

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * ✅ MAIN MIGRATION: Convert List structure to Map structure
//      * Old: Map<String, List<ApproachData>>
//      * New: Map<String, Map<String, ApproachData>>
//      */
//     @SuppressWarnings("unchecked")
//     public Map<String, Object> migrateListToMap() {
//         logger.info("🔄 ========================================");
//         logger.info("🔄 Starting CRITICAL Migration: List → Map");
//         logger.info("🔄 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int migratedUsers = 0;
//         int skippedUsers = 0;
//         int alreadyMigrated = 0;
//         int failedUsers = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Get raw documents from MongoDB
//             Query query = new Query();
//             List<Document> allUserDocs = mongoTemplate.find(query, Document.class, "user_approaches");
//             totalUsers = allUserDocs.size();

//             logger.info("📊 Found {} users to migrate", totalUsers);

//             for (Document userDoc : allUserDocs) {
//                 String userId = "unknown";
//                 try {
//                     userId = userDoc.getString("userId");
//                     if (userId == null) {
//                         userId = userDoc.getString("_id");
//                     }
                    
//                     logger.info("🔍 Processing user: {}", userId);
                    
//                     Object approachesObj = userDoc.get("approaches");

//                     if (approachesObj == null) {
//                         logger.info("⏭️  User {} has no approaches field, skipping", userId);
//                         skippedUsers++;
//                         continue;
//                     }

//                     // Check if it's a Document (Map structure)
//                     if (!(approachesObj instanceof Document)) {
//                         logger.warn("⚠️  User {} has unexpected approaches type: {}, skipping", 
//                             userId, approachesObj.getClass().getName());
//                         skippedUsers++;
//                         continue;
//                     }

//                     Document approachesDoc = (Document) approachesObj;
                    
//                     if (approachesDoc.isEmpty()) {
//                         logger.info("⏭️  User {} has empty approaches, skipping", userId);
//                         skippedUsers++;
//                         continue;
//                     }

//                     // ✅ FIXED: Use keySet() to safely iterate
//                     Set<String> questionIds = approachesDoc.keySet();
                    
//                     if (questionIds.isEmpty()) {
//                         logger.info("⏭️  User {} has no questions, skipping", userId);
//                         skippedUsers++;
//                         continue;
//                     }

//                     // Check structure of first question to determine if migration needed
//                     String firstQuestionId = questionIds.iterator().next();
//                     Object firstValue = approachesDoc.get(firstQuestionId);
                    
//                     logger.info("🔍 First question ID: {}, value type: {}", 
//                         firstQuestionId, firstValue.getClass().getName());

//                     // If first value is already a Document with UUID keys, it's migrated
//                     if (firstValue instanceof Document) {
//                         Document firstValueDoc = (Document) firstValue;
//                         if (!firstValueDoc.isEmpty()) {
//                             String firstKey = firstValueDoc.keySet().iterator().next();
//                             // UUID format check (contains hyphens)
//                             if (firstKey != null && firstKey.contains("-")) {
//                                 logger.info("✓  User {} already migrated (Map structure detected), skipping", userId);
//                                 alreadyMigrated++;
//                                 continue;
//                             }
//                         }
//                     }
                    
//                     // If first value is List, needs migration
//                     if (firstValue instanceof List) {
//                         logger.info("🔄 Migrating user {} (List structure detected)...", userId);
                        
//                         // Create new Map<String, Map<String, ApproachData>> structure
//                         Map<String, Map<String, Object>> newStructure = new HashMap<>();
//                         int totalApproachesCount = 0;

//                         for (String questionId : questionIds) {
//                             Object questionApproachesObj = approachesDoc.get(questionId);
                            
//                             if (!(questionApproachesObj instanceof List)) {
//                                 logger.warn("⚠️  Question {} has unexpected type: {}", 
//                                     questionId, questionApproachesObj.getClass().getName());
//                                 continue;
//                             }
                            
//                             List<Document> approachesList = (List<Document>) questionApproachesObj;
                            
//                             Map<String, Object> approachesMap = new HashMap<>();
//                             for (Document approach : approachesList) {
//                                 String approachId = approach.getString("id");
//                                 if (approachId == null) {
//                                     logger.warn("⚠️  Found approach without ID, skipping");
//                                     continue;
//                                 }
//                                 approachesMap.put(approachId, approach);
//                                 totalApproachesCount++;
//                             }
                            
//                             newStructure.put(questionId, approachesMap);
//                         }

//                         // Update document with new structure
//                         Query updateQuery = new Query(Criteria.where("userId").is(userId));
//                         Update update = new Update()
//                             .set("approaches", newStructure)
//                             .set("totalApproaches", totalApproachesCount)
//                             .set("lastUpdated", LocalDateTime.now());

//                         mongoTemplate.updateFirst(updateQuery, update, "user_approaches");

//                         logger.info("✅ Migrated user {}: {} total approaches across {} questions", 
//                             userId, totalApproachesCount, newStructure.size());
                        
//                         migratedUsers++;
//                     } else {
//                         logger.warn("⚠️  User {} has unknown structure type: {}", 
//                             userId, firstValue.getClass().getName());
//                         skippedUsers++;
//                     }

//                 } catch (Exception e) {
//                     String error = String.format("Failed to migrate user %s: %s", userId, e.getMessage());
//                     logger.error("❌ {}", error);
//                     logger.error("Stack trace:", e);
//                     errors.add(error);
//                     failedUsers++;
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("migratedUsers", migratedUsers);
//             result.put("alreadyMigrated", alreadyMigrated);
//             result.put("skippedUsers", skippedUsers);
//             result.put("failedUsers", failedUsers);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d migrated, %d already migrated, %d skipped, %d failed out of %d total users",
//                 migratedUsers, alreadyMigrated, skippedUsers, failedUsers, totalUsers));

//             logger.info("\n📈 Migration Summary:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   ✅ Migrated: {}", migratedUsers);
//             logger.info("   ✓  Already Migrated: {}", alreadyMigrated);
//             logger.info("   ⏭️  Skipped: {}", skippedUsers);
//             logger.info("   ❌ Failed: {}", failedUsers);
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
//      * ✅ Fix totalApproaches count for all users
//      */
//     public Map<String, Object> fixTotalApproachesCount() {
//         logger.info("🔧 ========================================");
//         logger.info("🔧 Fixing totalApproaches count for all users");
//         logger.info("🔧 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int fixedUsers = 0;
//         int alreadyCorrect = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Must fetch AFTER migration to use new structure
//             List<com.algoarena.model.UserApproaches> allUsers = userApproachesRepository.findAll();
//             totalUsers = allUsers.size();

//             logger.info("📊 Found {} users to check", totalUsers);

//             for (com.algoarena.model.UserApproaches user : allUsers) {
//                 try {
//                     int actualCount = user.getAllApproachesFlat().size();
//                     int storedCount = user.getTotalApproaches();

//                     if (actualCount != storedCount) {
//                         Query query = new Query(Criteria.where("userId").is(user.getUserId()));
//                         Update update = new Update()
//                             .set("totalApproaches", actualCount)
//                             .set("lastUpdated", LocalDateTime.now());

//                         mongoTemplate.updateFirst(query, update, com.algoarena.model.UserApproaches.class);

//                         logger.info("✅ Fixed user {}: {} -> {}", 
//                             user.getUserId(), storedCount, actualCount);
//                         fixedUsers++;
//                     } else {
//                         alreadyCorrect++;
//                     }

//                 } catch (Exception e) {
//                     String error = String.format("Failed to fix user %s: %s", 
//                         user.getUserId(), e.getMessage());
//                     logger.error("❌ {}", error);
//                     errors.add(error);
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("fixedUsers", fixedUsers);
//             result.put("alreadyCorrect", alreadyCorrect);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Fixed %d users, %d were already correct out of %d total",
//                 fixedUsers, alreadyCorrect, totalUsers));

//             logger.info("\n📈 Fix Summary:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   ✅ Fixed: {}", fixedUsers);
//             logger.info("   ✓  Already Correct: {}", alreadyCorrect);
//             logger.info("   ❌ Errors: {}", errors.size());

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Fix failed: " + e.getMessage());
//             logger.error("❌ Fix failed: {}", e.getMessage(), e);
//         }

//         return result;
//     }

//     /**
//      * ✅ Verify migration completed successfully
//      */
//     public Map<String, Object> verifyMigration() {
//         logger.info("🔍 Verifying migration...");

//         Map<String, Object> result = new HashMap<>();
//         List<String> issues = new ArrayList<>();

//         try {
//             List<com.algoarena.model.UserApproaches> allUsers = userApproachesRepository.findAll();
//             int totalUsers = allUsers.size();
//             int verifiedUsers = 0;
//             int usersWithIssues = 0;

//             for (com.algoarena.model.UserApproaches user : allUsers) {
//                 boolean hasIssue = false;

//                 // Check if approaches exist
//                 if (user.getApproaches() == null) {
//                     issues.add(String.format("User %s has null approaches", user.getUserId()));
//                     hasIssue = true;
//                 }

//                 // Verify totalApproaches count
//                 int actualCount = user.getAllApproachesFlat().size();
//                 if (user.getTotalApproaches() != actualCount) {
//                     issues.add(String.format("User %s: totalApproaches mismatch (stored=%d, actual=%d)", 
//                         user.getUserId(), user.getTotalApproaches(), actualCount));
//                     hasIssue = true;
//                 }

//                 if (hasIssue) {
//                     usersWithIssues++;
//                 } else {
//                     verifiedUsers++;
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("verifiedUsers", verifiedUsers);
//             result.put("usersWithIssues", usersWithIssues);
//             result.put("issues", issues);
//             result.put("migrationComplete", usersWithIssues == 0);
//             result.put("message", usersWithIssues == 0 
//                 ? "✅ Migration verified: All users have correct structure"
//                 : String.format("⚠️ Found issues with %d users", usersWithIssues));

//             logger.info("📊 Verification Summary:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   ✅ Verified: {}", verifiedUsers);
//             logger.info("   ⚠️  With Issues: {}", usersWithIssues);
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
//             List<com.algoarena.model.UserApproaches> allUsers = userApproachesRepository.findAll();
            
//             int totalUsers = allUsers.size();
//             int usersWithApproaches = 0;
//             int totalApproaches = 0;
//             int totalQuestions = 0;
            
//             for (com.algoarena.model.UserApproaches user : allUsers) {
//                 if (!user.getApproaches().isEmpty()) {
//                     usersWithApproaches++;
//                     totalApproaches += user.getAllApproachesFlat().size();
//                     totalQuestions += user.getApproaches().size();
//                 }
//             }

//             stats.put("totalUsers", totalUsers);
//             stats.put("usersWithApproaches", usersWithApproaches);
//             stats.put("totalApproaches", totalApproaches);
//             stats.put("totalQuestions", totalQuestions);
//             stats.put("avgApproachesPerUser", usersWithApproaches > 0 
//                 ? String.format("%.2f", (double) totalApproaches / usersWithApproaches) 
//                 : "0");
//             stats.put("avgQuestionsPerUser", usersWithApproaches > 0 
//                 ? String.format("%.2f", (double) totalQuestions / usersWithApproaches) 
//                 : "0");

//         } catch (Exception e) {
//             stats.put("error", "Failed to get stats: " + e.getMessage());
//         }

//         return stats;
//     }
// }