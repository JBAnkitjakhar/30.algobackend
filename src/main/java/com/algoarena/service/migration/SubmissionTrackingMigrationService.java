// // src/main/java/com/algoarena/service/migration/SubmissionTrackingMigrationService.java
// package com.algoarena.service.migration;

// import com.algoarena.model.SubmissionTracking;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.mongodb.core.query.Criteria;
// import org.springframework.data.mongodb.core.query.Query;
// import org.springframework.stereotype.Service;

// import java.time.LocalDate;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// public class SubmissionTrackingMigrationService {

//     private static final Logger logger = LoggerFactory.getLogger(SubmissionTrackingMigrationService.class);

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * ✅ Migrate ALL users: Convert LocalDate with timezone to simple date strings
//      * This fixes timezone shifting issues for all users
//      */
//     public Map<String, Object> migrateAllUsersToSimpleDates() {
//         logger.info("🔄 ========================================");
//         logger.info("🔄 Starting Date Format Migration for ALL Users");
//         logger.info("🔄 Converting LocalDate+Timezone → Simple Date String");
//         logger.info("🔄 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int migratedUsers = 0;
//         int skippedUsers = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             Query query = new Query();
//             List<SubmissionTracking> allTracking = mongoTemplate.find(query, SubmissionTracking.class);
//             totalUsers = allTracking.size();

//             logger.info("📊 Found {} users to check", totalUsers);

//             for (SubmissionTracking tracking : allTracking) {
//                 String userId = tracking.getUserId();
//                 try {
//                     List<SubmissionTracking.DailySubmission> history = tracking.getSubmissionHistory();
                    
//                     if (history.isEmpty()) {
//                         skippedUsers++;
//                         logger.info("⏭️  Skipped user {} (no submission history)", userId);
//                         continue;
//                     }

//                     // Re-save the document - MongoConfig converters will handle the conversion
//                     // Old format: LocalDate stored as DateTime with timezone
//                     // New format: LocalDate stored as simple string "YYYY-MM-DD"
//                     mongoTemplate.save(tracking);
                    
//                     migratedUsers++;
//                     logger.info("✅ Migrated user {}: {} dates converted to simple format", 
//                             userId, history.size());

//                 } catch (Exception e) {
//                     String error = String.format("Failed to migrate user %s: %s", userId, e.getMessage());
//                     logger.error("❌ {}", error);
//                     errors.add(error);
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("migratedUsers", migratedUsers);
//             result.put("skippedUsers", skippedUsers);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                     "Migration completed: %d users processed, %d migrated to simple date format, %d skipped",
//                     totalUsers, migratedUsers, skippedUsers));

//             logger.info("\n📈 Date Format Migration Summary:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   ✅ Migrated: {}", migratedUsers);
//             logger.info("   ⏭️  Skipped (empty): {}", skippedUsers);
//             if (!errors.isEmpty()) {
//                 logger.info("   ❌ Errors: {}", errors.size());
//                 errors.forEach(e -> logger.info("      - {}", e));
//             }
//             logger.info("✅ All users now use timezone-independent date strings!");

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             logger.error("❌ Migration failed: {}", e.getMessage(), e);
//         }

//         return result;
//     }

//     /**
//      * ✅ Verify user's submission history (check for duplicates and data integrity)
//      */
//     public Map<String, Object> verifyUserSubmissions(String userId) {
//         logger.info("🔍 Verifying submission history for user: {}", userId);

//         Map<String, Object> result = new HashMap<>();
//         List<String> duplicateDates = new ArrayList<>();
//         List<String> issues = new ArrayList<>();

//         try {
//             Query query = new Query(Criteria.where("userId").is(userId));
//             SubmissionTracking tracking = mongoTemplate.findOne(query, SubmissionTracking.class);

//             if (tracking == null) {
//                 result.put("success", false);
//                 result.put("error", "User not found");
//                 result.put("message", "No submission tracking found for user: " + userId);
//                 return result;
//             }

//             List<SubmissionTracking.DailySubmission> history = tracking.getSubmissionHistory();
//             int totalEntries = history.size();
//             int totalSubmissions = history.stream()
//                     .mapToInt(SubmissionTracking.DailySubmission::getCount)
//                     .sum();

//             // Find duplicate dates
//             Map<LocalDate, Long> dateOccurrences = history.stream()
//                     .collect(Collectors.groupingBy(
//                             SubmissionTracking.DailySubmission::getDate,
//                             Collectors.counting()));

//             dateOccurrences.entrySet().stream()
//                     .filter(entry -> entry.getValue() > 1)
//                     .sorted(Map.Entry.comparingByKey())
//                     .forEach(entry -> {
//                         String detail = String.format("%s appears %d times", entry.getKey(), entry.getValue());
//                         duplicateDates.add(detail);
//                         logger.warn("   ⚠️  {}", detail);
//                     });

//             int uniqueDates = dateOccurrences.size();
//             boolean hasDuplicates = !duplicateDates.isEmpty();

//             // Check if data is correctly ordered (oldest first)
//             boolean correctOrder = true;
//             for (int i = 0; i < history.size() - 1; i++) {
//                 if (history.get(i).getDate().isAfter(history.get(i + 1).getDate())) {
//                     correctOrder = false;
//                     issues.add("Data is not sorted correctly (should be oldest first)");
//                     break;
//                 }
//             }

//             // Determine data integrity
//             String dataIntegrity;
//             if (!hasDuplicates && correctOrder) {
//                 dataIntegrity = "GOOD";
//                 logger.info("✅ Data integrity: GOOD");
//             } else {
//                 dataIntegrity = "NEEDS_FIX";
//                 logger.warn("⚠️  Data integrity: NEEDS FIX");
//             }

//             result.put("success", true);
//             result.put("userId", userId);
//             result.put("totalEntries", totalEntries);
//             result.put("uniqueDates", uniqueDates);
//             result.put("totalSubmissions", totalSubmissions);
//             result.put("hasDuplicates", hasDuplicates);
//             result.put("duplicateCount", totalEntries - uniqueDates);
//             result.put("duplicateDates", duplicateDates);
//             result.put("correctOrder", correctOrder);
//             result.put("dataIntegrity", dataIntegrity);
//             result.put("issues", issues);
//             result.put("message", hasDuplicates
//                     ? String.format("⚠️ Found %d duplicate entries across %d dates",
//                             totalEntries - uniqueDates, duplicateDates.size())
//                     : "✅ No duplicates found - submission history is clean");

//             logger.info("📊 Verification Results:");
//             logger.info("   Total Entries: {}", totalEntries);
//             logger.info("   Unique Dates: {}", uniqueDates);
//             logger.info("   Total Submissions: {}", totalSubmissions);
//             logger.info("   Correct Order: {}", correctOrder);
//             logger.info("   Status: {}", hasDuplicates ? "⚠️ Has Duplicates" : "✅ Clean");

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Verification failed: " + e.getMessage());
//             logger.error("❌ Verification failed: {}", e.getMessage(), e);
//         }

//         return result;
//     }

//     /**
//      * ✅ Get migration statistics for all users
//      */
//     public Map<String, Object> getMigrationStats() {
//         logger.info("📊 Getting submission tracking statistics...");

//         Map<String, Object> stats = new HashMap<>();

//         try {
//             Query query = new Query();
//             List<SubmissionTracking> allTracking = mongoTemplate.find(query, SubmissionTracking.class);

//             int totalUsers = allTracking.size();
//             int usersWithData = 0;
//             int usersWithDuplicates = 0;
//             int totalSubmissions = 0;
//             int totalDays = 0;

//             for (SubmissionTracking tracking : allTracking) {
//                 List<SubmissionTracking.DailySubmission> history = tracking.getSubmissionHistory();
                
//                 if (history.isEmpty()) {
//                     continue;
//                 }

//                 usersWithData++;
                
//                 int userSubmissions = history.stream()
//                         .mapToInt(SubmissionTracking.DailySubmission::getCount)
//                         .sum();
                
//                 totalSubmissions += userSubmissions;
                
//                 long uniqueDates = history.stream()
//                         .map(SubmissionTracking.DailySubmission::getDate)
//                         .distinct()
//                         .count();
                
//                 totalDays += uniqueDates;
                
//                 if (history.size() > uniqueDates) {
//                     usersWithDuplicates++;
//                 }
//             }

//             stats.put("totalUsers", totalUsers);
//             stats.put("usersWithData", usersWithData);
//             stats.put("usersWithDuplicates", usersWithDuplicates);
//             stats.put("totalSubmissions", totalSubmissions);
//             stats.put("totalActiveDays", totalDays);
//             stats.put("needsMigration", usersWithDuplicates > 0);

//             logger.info("📊 Statistics:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   Users with Data: {}", usersWithData);
//             logger.info("   Users with Duplicates: {}", usersWithDuplicates);
//             logger.info("   Total Submissions: {}", totalSubmissions);
//             logger.info("   Total Active Days: {}", totalDays);

//         } catch (Exception e) {
//             stats.put("error", "Failed to get stats: " + e.getMessage());
//             logger.error("❌ Failed to get stats: {}", e.getMessage(), e);
//         }

//         return stats;
//     }
// }