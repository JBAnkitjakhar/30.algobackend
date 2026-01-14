// // src/main/java/com/algoarena/service/migration/SubmissionHistoryMigrationService.java
// package com.algoarena.service.migration;

// import com.algoarena.model.SubmissionTracking;
// import com.algoarena.model.SubmissionTracking.DailySubmission;
// import com.algoarena.model.UserApproaches;
// import com.algoarena.model.UserApproaches.ApproachData;
// import com.algoarena.repository.SubmissionTrackingRepository;
// import com.algoarena.repository.UserApproachesRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDate;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// @Transactional
// public class SubmissionHistoryMigrationService {

//     @Autowired
//     private UserApproachesRepository userApproachesRepository;

//     @Autowired
//     private SubmissionTrackingRepository submissionTrackingRepository;

//     /**
//      * Build submission history from existing approaches
//      * Creates submission tracking from approach creation dates
//      */
//     public Map<String, Object> migrateFromApproaches() {
//         System.out.println("🔄 Starting migration: Building submission history from approaches...");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int migratedUsers = 0;
//         int totalApproaches = 0;
//         int totalDays = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Get all users with approaches
//             List<UserApproaches> allUserApproaches = userApproachesRepository.findAll();
//             totalUsers = allUserApproaches.size();

//             System.out.println("📊 Found " + totalUsers + " users with approaches");

//             for (UserApproaches userApproaches : allUserApproaches) {
//                 try {
//                     String userId = userApproaches.getUserId();

//                     // Get all approaches for this user
//                     List<ApproachData> allApproaches = userApproaches.getAllApproachesFlat();

//                     if (allApproaches.isEmpty()) {
//                         continue;
//                     }

//                     // Group approaches by date
//                     // MongoDB dates are already in correct timezone, just extract the date
//                     Map<LocalDate, Long> countByDate = allApproaches.stream()
//                             .collect(Collectors.groupingBy(
//                                     approach -> approach.getCreatedAt().toLocalDate(),
//                                     Collectors.counting()));

//                     // Create submission tracking
//                     SubmissionTracking tracking = new SubmissionTracking();
//                     tracking.setUserId(userId);

//                     // Convert to DailySubmission list and sort by date (oldest first)
//                     List<DailySubmission> dailySubmissions = countByDate.entrySet().stream()
//                             .sorted(Map.Entry.comparingByKey()) // Sort by date ascending
//                             .map(entry -> new DailySubmission(entry.getKey(), entry.getValue().intValue()))
//                             .collect(Collectors.toList());

//                     tracking.setSubmissionHistory(dailySubmissions);

//                     // Save to database
//                     submissionTrackingRepository.save(tracking);

//                     migratedUsers++;
//                     totalApproaches += allApproaches.size();
//                     totalDays += dailySubmissions.size();

//                     System.out.println("✅ Migrated user " + userId + ": " +
//                             allApproaches.size() + " approaches across " +
//                             dailySubmissions.size() + " days");

//                 } catch (Exception e) {
//                     String error = "Error migrating user " + userApproaches.getUserId() + ": " + e.getMessage();
//                     errors.add(error);
//                     System.err.println("❌ " + error);
//                     e.printStackTrace();
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("migratedUsers", migratedUsers);
//             result.put("totalApproaches", totalApproaches);
//             result.put("totalDays", totalDays);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                     "Migration completed: %d users migrated (%d approaches across %d days)",
//                     migratedUsers, totalApproaches, totalDays));

//             System.out.println("\n📈 Migration Summary:");
//             System.out.println("   Total Users: " + totalUsers);
//             System.out.println("   ✅ Migrated: " + migratedUsers);
//             System.out.println("   📝 Total Approaches: " + totalApproaches);
//             System.out.println("   📅 Total Days: " + totalDays);
//             System.out.println("   ❌ Errors: " + errors.size());

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             System.err.println("❌ Migration failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }
// }