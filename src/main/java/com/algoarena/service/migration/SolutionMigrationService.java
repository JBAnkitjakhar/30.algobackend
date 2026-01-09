// // src/main/java/com/algoarena/service/migration/SolutionMigrationService.java
// package com.algoarena.service.migration;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.data.mongodb.core.query.Criteria;
// import org.springframework.data.mongodb.core.query.Query;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.*;

// @Service
// @Transactional
// public class SolutionMigrationService {

//     @Autowired
//     private MongoTemplate mongoTemplate;

//     /**
//      * Migrate all existing solutions from old CodeSnippet format to new codeTemplates Map format
//      * Old format: { codeSnippet: { language: "java", code: "...", description: "..." } }
//      * New format: { codeTemplates: { "java": ["..."] } }
//      */
//     @CacheEvict(value = {
//             "adminSolutionsSummary",
//             "solutionDetail",
//             "questionSolutions"
//     }, allEntries = true)
//     public Map<String, Object> migrateSolutionsToNewFormat() {
//         System.out.println("🔄 Starting solution migration...");
        
//         Map<String, Object> result = new HashMap<>();
//         int totalSolutions = 0;
//         int migratedCount = 0;
//         int skippedCount = 0;
//         int errorCount = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Find all solutions with old codeSnippet field
//             Query query = new Query(Criteria.where("codeSnippet").exists(true));
//             List<org.bson.Document> oldFormatSolutions = mongoTemplate.find(
//                 query, 
//                 org.bson.Document.class, 
//                 "solutions"
//             );

//             totalSolutions = oldFormatSolutions.size();
//             System.out.println("📊 Found " + totalSolutions + " solutions with old format");

//             for (org.bson.Document doc : oldFormatSolutions) {
//                 try {
//                     String solutionId = doc.getObjectId("_id").toString();
//                     Object codeSnippetObj = doc.get("codeSnippet");

//                     // Check if already has new format
//                     if (doc.containsKey("codeTemplates")) {
//                         System.out.println("⏭️  Solution " + solutionId + " already migrated, skipping...");
//                         skippedCount++;
//                         continue;
//                     }

//                     // Extract old format data
//                     if (codeSnippetObj instanceof org.bson.Document) {
//                         org.bson.Document codeSnippet = (org.bson.Document) codeSnippetObj;
                        
//                         String language = codeSnippet.getString("language");
//                         String code = codeSnippet.getString("code");

//                         // Skip if no language or code
//                         if (language == null || language.trim().isEmpty() || 
//                             code == null || code.trim().isEmpty()) {
//                             System.out.println("⚠️  Solution " + solutionId + " has empty language/code, skipping...");
//                             skippedCount++;
//                             continue;
//                         }

//                         // Create new codeTemplates map
//                         Map<String, List<String>> codeTemplates = new HashMap<>();
//                         codeTemplates.put(language.toLowerCase().trim(), List.of(code));

//                         // Create a new document with proper field ordering
//                         org.bson.Document newDoc = new org.bson.Document();
                        
//                         // Add fields in the correct order matching the Solution model
//                         newDoc.append("_id", doc.getObjectId("_id"));
//                         newDoc.append("questionId", doc.getString("questionId"));
//                         newDoc.append("content", doc.getString("content"));
//                         newDoc.append("driveLink", doc.getString("driveLink"));
//                         newDoc.append("youtubeLink", doc.getString("youtubeLink"));
//                         newDoc.append("imageUrls", doc.get("imageUrls"));
//                         newDoc.append("visualizerFileIds", doc.get("visualizerFileIds"));
                        
//                         // ✅ Add codeTemplates here (after visualizerFileIds, before createdByName)
//                         newDoc.append("codeTemplates", codeTemplates);
                        
//                         newDoc.append("createdByName", doc.getString("createdByName"));
//                         newDoc.append("createdAt", doc.get("createdAt"));
//                         newDoc.append("updatedAt", LocalDateTime.now());
                        
//                         // Add any other fields that might exist
//                         for (String key : doc.keySet()) {
//                             if (!newDoc.containsKey(key) && !key.equals("codeSnippet")) {
//                                 newDoc.append(key, doc.get(key));
//                             }
//                         }

//                         // Replace the entire document
//                         mongoTemplate.remove(new Query(Criteria.where("_id").is(doc.getObjectId("_id"))), "solutions");
//                         mongoTemplate.insert(newDoc, "solutions");
                        
//                         System.out.println("✅ Migrated solution " + solutionId + " - Language: " + language);
//                         migratedCount++;
//                     } else {
//                         System.out.println("⚠️  Solution " + solutionId + " has invalid codeSnippet format, skipping...");
//                         skippedCount++;
//                     }

//                 } catch (Exception e) {
//                     errorCount++;
//                     String errorMsg = "Error migrating solution: " + e.getMessage();
//                     errors.add(errorMsg);
//                     System.err.println("❌ " + errorMsg);
//                     e.printStackTrace();
//                 }
//             }

//             // Summary
//             result.put("success", true);
//             result.put("totalSolutions", totalSolutions);
//             result.put("migratedCount", migratedCount);
//             result.put("skippedCount", skippedCount);
//             result.put("errorCount", errorCount);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Migration completed: %d migrated, %d skipped, %d errors",
//                 migratedCount, skippedCount, errorCount
//             ));

//             System.out.println("\n📈 Migration Summary:");
//             System.out.println("   Total: " + totalSolutions);
//             System.out.println("   ✅ Migrated: " + migratedCount);
//             System.out.println("   ⏭️  Skipped: " + skippedCount);
//             System.out.println("   ❌ Errors: " + errorCount);
//             System.out.println("✨ All caches cleared!");

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Migration failed: " + e.getMessage());
//             System.err.println("❌ Migration failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }

//     /**
//      * Reorder fields in existing migrated solutions to match model structure
//      * This is for solutions that already have codeTemplates but in wrong position
//      */
//     @CacheEvict(value = {
//             "adminSolutionsSummary",
//             "solutionDetail",
//             "questionSolutions"
//     }, allEntries = true)
//     public Map<String, Object> reorderCodeTemplatesField() {
//         System.out.println("🔄 Starting field reordering for codeTemplates...");
        
//         Map<String, Object> result = new HashMap<>();
//         int totalSolutions = 0;
//         int reorderedCount = 0;
//         int errorCount = 0;
//         List<String> errors = new ArrayList<>();

//         try {
//             // Find all solutions that have codeTemplates field
//             Query query = new Query(Criteria.where("codeTemplates").exists(true));
//             List<org.bson.Document> solutions = mongoTemplate.find(
//                 query, 
//                 org.bson.Document.class, 
//                 "solutions"
//             );

//             totalSolutions = solutions.size();
//             System.out.println("📊 Found " + totalSolutions + " solutions with codeTemplates");

//             for (org.bson.Document doc : solutions) {
//                 try {
//                     String solutionId = doc.getObjectId("_id").toString();

//                     // Create a new document with proper field ordering
//                     org.bson.Document newDoc = new org.bson.Document();
                    
//                     // Add fields in the correct order
//                     newDoc.append("_id", doc.getObjectId("_id"));
//                     newDoc.append("questionId", doc.getString("questionId"));
//                     newDoc.append("content", doc.getString("content"));
//                     newDoc.append("driveLink", doc.getString("driveLink"));
//                     newDoc.append("youtubeLink", doc.getString("youtubeLink"));
//                     newDoc.append("imageUrls", doc.get("imageUrls"));
//                     newDoc.append("visualizerFileIds", doc.get("visualizerFileIds"));
                    
//                     // ✅ Add codeTemplates here (correct position)
//                     newDoc.append("codeTemplates", doc.get("codeTemplates"));
                    
//                     newDoc.append("createdByName", doc.getString("createdByName"));
//                     newDoc.append("createdAt", doc.get("createdAt"));
//                     newDoc.append("updatedAt", LocalDateTime.now());
                    
//                     // Add any other fields
//                     for (String key : doc.keySet()) {
//                         if (!newDoc.containsKey(key)) {
//                             newDoc.append(key, doc.get(key));
//                         }
//                     }

//                     // Replace the document
//                     mongoTemplate.remove(new Query(Criteria.where("_id").is(doc.getObjectId("_id"))), "solutions");
//                     mongoTemplate.insert(newDoc, "solutions");
                    
//                     System.out.println("✅ Reordered fields for solution " + solutionId);
//                     reorderedCount++;

//                 } catch (Exception e) {
//                     errorCount++;
//                     String errorMsg = "Error reordering solution: " + e.getMessage();
//                     errors.add(errorMsg);
//                     System.err.println("❌ " + errorMsg);
//                     e.printStackTrace();
//                 }
//             }

//             result.put("success", true);
//             result.put("totalSolutions", totalSolutions);
//             result.put("reorderedCount", reorderedCount);
//             result.put("errorCount", errorCount);
//             result.put("errors", errors);
//             result.put("message", String.format(
//                 "Reordering completed: %d reordered, %d errors",
//                 reorderedCount, errorCount
//             ));

//             System.out.println("\n📈 Reordering Summary:");
//             System.out.println("   Total: " + totalSolutions);
//             System.out.println("   ✅ Reordered: " + reorderedCount);
//             System.out.println("   ❌ Errors: " + errorCount);
//             System.out.println("✨ All caches cleared!");

//         } catch (Exception e) {
//             result.put("success", false);
//             result.put("error", "Reordering failed: " + e.getMessage());
//             System.err.println("❌ Reordering failed: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return result;
//     }
// }