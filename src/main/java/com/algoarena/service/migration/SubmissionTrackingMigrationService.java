// // src/main/java/com/algoarena/service/migration/SubmissionTrackingMigrationService.java

// package com.algoarena.service.migration;

// import com.algoarena.model.SubmissionTracking;
// import com.algoarena.repository.SubmissionTrackingRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.time.LocalDate;
// import java.util.*;

// @Service
// public class SubmissionTrackingMigrationService {

//     private static final Logger logger = LoggerFactory.getLogger(SubmissionTrackingMigrationService.class);
 

//     @Autowired
//     private SubmissionTrackingRepository submissionTrackingRepository;

//     /**
//      * ✅ Migrate submission tracking dates from DateTime to LocalDate (UTC)
//      * Converts all existing DateTime entries to pure LocalDate
//      */
//     public Map<String, Object> fixSubmissionDates() {
//         logger.info("🔄 ========================================");
//         logger.info("🔄 Starting Submission Date Migration");
//         logger.info("🔄 ========================================");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int totalSubmissionDates = 0;
//         int fixedDates = 0;
//         int skippedUsers = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             List<SubmissionTracking> allTracking = submissionTrackingRepository.findAll();
//             totalUsers = allTracking.size();

//             logger.info("📊 Found {} users to process", totalUsers);

//             for (SubmissionTracking tracking : allTracking) {
//                 try {
//                     String userId = tracking.getUserId();
                    
//                     if (tracking.getSubmissionHistory() == null || tracking.getSubmissionHistory().isEmpty()) {
//                         logger.info("⏭️  User {} has no submission history, skipping", userId);
//                         skippedUsers++;
//                         continue;
//                     }

//                     boolean modified = false;

//                     for (SubmissionTracking.DailySubmission submission : tracking.getSubmissionHistory()) {
//                         totalSubmissionDates++;
                        
//                         LocalDate currentDate = submission.getDate();
                        
//                         if (currentDate == null) {
//                             logger.warn("⚠️  Found null date for user {}, skipping entry", userId);
//                             continue;
//                         }
                        
//                         // Reconstruct date to ensure it's pure LocalDate (no time component)
//                         LocalDate fixedDate = LocalDate.of(
//                             currentDate.getYear(),
//                             currentDate.getMonth(),
//                             currentDate.getDayOfMonth()
//                         );
                        
//                         // Update the date to ensure no time component
//                         submission.setDate(fixedDate);
//                         modified = true;
//                         fixedDates++;
//                     }

//                     if (modified) {
//                         // Save the updated tracking document
//                         submissionTrackingRepository.save(tracking);
//                         logger.info("✅ Fixed {} submission dates for user: {}", 
//                                   tracking.getSubmissionHistory().size(), userId);
//                     }

//                 } catch (Exception e) {
//                     String error = String.format("Failed to migrate user %s: %s", 
//                                                 tracking.getUserId(), e.getMessage());
//                     logger.error("❌ {}", error);
//                     errors.add(error);
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("totalSubmissionDates", totalSubmissionDates);
//             result.put("fixedDates", fixedDates);
//             result.put("skippedUsers", skippedUsers);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d users processed, %d submission dates fixed, %d users skipped",
//                 totalUsers, fixedDates, skippedUsers));

//             logger.info("\n📈 Migration Summary:");
//             logger.info("   Total Users: {}", totalUsers);
//             logger.info("   Total Submission Dates: {}", totalSubmissionDates);
//             logger.info("   ✅ Fixed Dates: {}", fixedDates);
//             logger.info("   ⏭️  Skipped Users: {}", skippedUsers);
//             logger.info("   ❌ Errors: {}", errors.size());
            
//             if (!errors.isEmpty()) {
//                 logger.info("   Error Details:");
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
//      * ✅ Get submission tracking statistics (preview before migration)
//      */
//     public Map<String, Object> getSubmissionStats() {
//         logger.info("📊 Getting submission tracking statistics...");

//         Map<String, Object> stats = new HashMap<>();

//         try {
//             List<SubmissionTracking> allTracking = submissionTrackingRepository.findAll();
            
//             int totalUsers = allTracking.size();
//             int usersWithSubmissions = 0;
//             int totalSubmissions = 0;
            
//             for (SubmissionTracking tracking : allTracking) {
//                 if (tracking.getSubmissionHistory() != null && !tracking.getSubmissionHistory().isEmpty()) {
//                     usersWithSubmissions++;
//                     totalSubmissions += tracking.getSubmissionHistory().size();
//                 }
//             }

//             stats.put("success", true);
//             stats.put("totalUsers", totalUsers);
//             stats.put("usersWithSubmissions", usersWithSubmissions);
//             stats.put("totalSubmissionDates", totalSubmissions);
//             stats.put("avgSubmissionsPerUser", usersWithSubmissions > 0 
//                 ? String.format("%.2f", (double) totalSubmissions / usersWithSubmissions) 
//                 : "0");
//             stats.put("message", "Use POST /fix-submission-dates to migrate");

//             logger.info("📊 Stats: {} users, {} with submissions, {} total dates", 
//                        totalUsers, usersWithSubmissions, totalSubmissions);

//         } catch (Exception e) {
//             stats.put("success", false);
//             stats.put("error", "Failed to get stats: " + e.getMessage());
//             logger.error("❌ Failed to get stats: {}", e.getMessage());
//         }

//         return stats;
//     }
// }