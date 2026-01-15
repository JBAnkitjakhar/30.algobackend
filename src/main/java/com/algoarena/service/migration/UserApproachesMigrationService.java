// // src/main/java/com/algoarena/service/migration/UserApproachesMigrationService.java
// package com.algoarena.service.migration;

// import com.algoarena.model.UserApproaches;
// import com.algoarena.repository.UserApproachesRepository;
// import org.bson.Document;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.mongodb.core.query.Criteria;
// import org.springframework.data.mongodb.core.query.Query;
// import org.springframework.data.mongodb.core.query.Update;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.*;

// @Service
// @Transactional
// public class UserApproachesMigrationService {

//     @Autowired
//     private UserApproachesRepository userApproachesRepository;

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * Remove version field from all UserApproaches documents
//      */
//     public Map<String, Object> removeVersionField() {
//         System.out.println("🔄 Starting migration: Removing version field from UserApproaches...");

//         Map<String, Object> result = new HashMap<>();
//         int totalUsers = 0;
//         int updatedUsers = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Get all user approaches documents
//             List<UserApproaches> allApproaches = userApproachesRepository.findAll();
//             totalUsers = allApproaches.size();

//             System.out.println("📊 Found " + totalUsers + " user approaches documents");

//             // Remove version field from all documents
//             Query query = new Query();
//             Update update = new Update().unset("version");
            
//             com.mongodb.client.result.UpdateResult updateResult = mongoTemplate.updateMulti(
//                 query, 
//                 update, 
//                 UserApproaches.class
//             );

//             updatedUsers = (int) updateResult.getModifiedCount();

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("updatedUsers", updatedUsers);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                     "Migration completed: Removed version field from %d/%d users",
//                     updatedUsers, totalUsers));

//             System.out.println("\n📈 Migration Summary:");
//             System.out.println("   Total Users: " + totalUsers);
//             System.out.println("   ✅ Updated: " + updatedUsers);
//             System.out.println("   ❌ Errors: " + errors.size());

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             System.err.println("❌ Migration failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }

//     /**
//      * Verify migration - check if any documents still have version field
//      */
//     public Map<String, Object> verifyMigration() {
//         System.out.println("🔍 Verifying migration...");

//         Map<String, Object> result = new HashMap<>();
        
//         try {
//             // Count total documents
//             long totalUsers = mongoTemplate.count(new Query(), UserApproaches.class);
            
//             // Count documents that still have version field
//             Query queryWithVersion = new Query(Criteria.where("version").exists(true));
//             long withVersion = mongoTemplate.count(queryWithVersion, UserApproaches.class);

//             // If any found, log them
//             if (withVersion > 0) {
//                 List<Document> docsWithVersion = mongoTemplate.find(
//                     queryWithVersion, 
//                     Document.class, 
//                     "user_approaches"
//                 );
                
//                 System.out.println("⚠️ Documents still with version field:");
//                 for (Document doc : docsWithVersion) {
//                     System.out.println("   User: " + doc.getString("userId") + 
//                                      ", Version: " + doc.get("version"));
//                 }
//             }

//             result.put("success", true);
//             result.put("totalUsers", totalUsers);
//             result.put("usersWithVersion", withVersion);
//             result.put("migrationComplete", withVersion == 0);
//             result.put("message", withVersion == 0 
//                 ? "✅ Migration verified: No version fields found"
//                 : "⚠️ Found " + withVersion + " users still with version field");

//             System.out.println(result.get("message"));

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Verification failed: " + e.getMessage());
//             System.err.println("❌ Verification failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }
// }