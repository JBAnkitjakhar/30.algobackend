// // src/main/java/com/algoarena/service/migration/ApproachMigrationService.java
// package com.algoarena.service.migration;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.mongodb.core.query.Criteria;
// import org.springframework.data.mongodb.core.query.Query;
// import org.springframework.data.mongodb.core.query.Update;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.*;

// @Service
// @Transactional
// public class ApproachMigrationService {

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     @SuppressWarnings("unchecked")
//     public Map<String, Object> addComplexityDescriptionField() {
//         System.out.println("🔄 Starting approach complexity description migration...");
        
//         Map<String, Object> result = new HashMap<>();
//         int totalApproaches = 0;
//         int migratedCount = 0;
//         int skippedCount = 0;
//         int errorCount = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             List<org.bson.Document> allUserApproaches = mongoTemplate.findAll(
//                 org.bson.Document.class, 
//                 "user_approaches"
//             );

//             System.out.println("📊 Found " + allUserApproaches.size() + " user approach documents");

//             for (org.bson.Document userDoc : allUserApproaches) {
//                 try {
//                     String userId = userDoc.getString("userId");
//                     Object approachesObj = userDoc.get("approaches");

//                     if (approachesObj == null) {
//                         continue;
//                     }

//                     Map<String, List<org.bson.Document>> approaches = 
//                         (Map<String, List<org.bson.Document>>) approachesObj;

//                     if (approaches.isEmpty()) {
//                         continue;
//                     }

//                     boolean documentModified = false;

//                     for (Map.Entry<String, List<org.bson.Document>> entry : approaches.entrySet()) {
//                         List<org.bson.Document> approachList = entry.getValue();

//                         for (org.bson.Document approach : approachList) {
//                             totalApproaches++;
                            
//                             org.bson.Document complexityAnalysis = 
//                                 (org.bson.Document) approach.get("complexityAnalysis");

//                             if (complexityAnalysis != null) {
//                                 if (complexityAnalysis.containsKey("complexityDescription")) {
//                                     skippedCount++;
//                                     continue;
//                                 }

//                                 complexityAnalysis.put("complexityDescription", null);
//                                 documentModified = true;
//                                 migratedCount++;
                                
//                                 System.out.println("✅ Added complexityDescription to approach: " + 
//                                     approach.getString("id"));
//                             }
//                         }
//                     }

//                     if (documentModified) {
//                         Query query = new Query(Criteria.where("userId").is(userId));
//                         Update update = new Update()
//                             .set("approaches", approaches)
//                             .set("lastUpdated", LocalDateTime.now());
                        
//                         mongoTemplate.updateFirst(query, update, "user_approaches");
//                     }

//                 } catch (Exception e) {
//                     errorCount++;
//                     String errorMsg = "Error migrating user approaches: " + e.getMessage();
//                     errors.add(errorMsg);
//                     System.err.println("❌ " + errorMsg);
//                     e.printStackTrace();
//                 }
//             }

//             result.put("success", true);
//             result.put("totalApproaches", totalApproaches);
//             result.put("migratedCount", migratedCount);
//             result.put("skippedCount", skippedCount);
//             result.put("errorCount", errorCount);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d migrated, %d skipped, %d errors",
//                 migratedCount, skippedCount, errorCount
//             ));

//             System.out.println("\n📈 Migration Summary:");
//             System.out.println("   Total: " + totalApproaches);
//             System.out.println("   ✅ Migrated: " + migratedCount);
//             System.out.println("   ⏭️  Skipped: " + skippedCount);
//             System.out.println("   ❌ Errors: " + errorCount);

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             System.err.println("❌ Migration failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }
// }