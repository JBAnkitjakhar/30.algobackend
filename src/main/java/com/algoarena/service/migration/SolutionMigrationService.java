// src/main/java/com/algoarena/service/migration/SolutionMigrationService.java
package com.algoarena.service.migration;

import com.algoarena.repository.SolutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SolutionMigrationService {

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Migrate all existing solutions from old CodeSnippet format to new codeTemplates Map format
     * Old format: { codeSnippet: { language: "java", code: "...", description: "..." } }
     * New format: { codeTemplates: { "java": ["..."] } }
     */
    @CacheEvict(value = {
            "adminSolutionsSummary",
            "solutionDetail",
            "questionSolutions"
    }, allEntries = true)
    public Map<String, Object> migrateSolutionsToNewFormat() {
        System.out.println("🔄 Starting solution migration...");
        
        Map<String, Object> result = new HashMap<>();
        int totalSolutions = 0;
        int migratedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Find all solutions with old codeSnippet field
            Query query = new Query(Criteria.where("codeSnippet").exists(true));
            List<org.bson.Document> oldFormatSolutions = mongoTemplate.find(
                query, 
                org.bson.Document.class, 
                "solutions"
            );

            totalSolutions = oldFormatSolutions.size();
            System.out.println("📊 Found " + totalSolutions + " solutions with old format");

            for (org.bson.Document doc : oldFormatSolutions) {
                try {
                    String solutionId = doc.getObjectId("_id").toString();
                    Object codeSnippetObj = doc.get("codeSnippet");

                    // Check if already has new format
                    if (doc.containsKey("codeTemplates")) {
                        System.out.println("⏭️  Solution " + solutionId + " already migrated, skipping...");
                        skippedCount++;
                        continue;
                    }

                    // Extract old format data
                    if (codeSnippetObj instanceof org.bson.Document) {
                        org.bson.Document codeSnippet = (org.bson.Document) codeSnippetObj;
                        
                        String language = codeSnippet.getString("language");
                        String code = codeSnippet.getString("code");
                        // Description is ignored in new format

                        // Skip if no language or code
                        if (language == null || language.trim().isEmpty() || 
                            code == null || code.trim().isEmpty()) {
                            System.out.println("⚠️  Solution " + solutionId + " has empty language/code, skipping...");
                            skippedCount++;
                            continue;
                        }

                        // Create new codeTemplates map
                        Map<String, List<String>> codeTemplates = new HashMap<>();
                        codeTemplates.put(language.toLowerCase().trim(), List.of(code));

                        // Update the document
                        Query updateQuery = new Query(Criteria.where("_id").is(doc.getObjectId("_id")));
                        Update update = new Update()
                            .set("codeTemplates", codeTemplates)
                            .unset("codeSnippet")  // Remove old field
                            .set("updatedAt", LocalDateTime.now());

                        mongoTemplate.updateFirst(updateQuery, update, "solutions");
                        
                        System.out.println("✅ Migrated solution " + solutionId + " - Language: " + language);
                        migratedCount++;
                    } else {
                        System.out.println("⚠️  Solution " + solutionId + " has invalid codeSnippet format, skipping...");
                        skippedCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error migrating solution: " + e.getMessage();
                    errors.add(errorMsg);
                    System.err.println("❌ " + errorMsg);
                }
            }

            // Summary
            result.put("success", true);
            result.put("totalSolutions", totalSolutions);
            result.put("migratedCount", migratedCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);
            result.put("message", String.format(
                "Migration completed: %d migrated, %d skipped, %d errors",
                migratedCount, skippedCount, errorCount
            ));

            System.out.println("\n📈 Migration Summary:");
            System.out.println("   Total: " + totalSolutions);
            System.out.println("   ✅ Migrated: " + migratedCount);
            System.out.println("   ⏭️  Skipped: " + skippedCount);
            System.out.println("   ❌ Errors: " + errorCount);
            System.out.println("✨ All caches cleared!");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Migration failed: " + e.getMessage());
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Rollback migration - converts new format back to old format (for emergency use)
     * WARNING: This will only keep the FIRST code template per language!
     */
    @CacheEvict(value = {
            "adminSolutionsSummary",
            "solutionDetail",
            "questionSolutions"
    }, allEntries = true)
    public Map<String, Object> rollbackMigration() {
        System.out.println("🔄 Starting migration rollback...");
        
        Map<String, Object> result = new HashMap<>();
        int totalSolutions = 0;
        int rolledBackCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Find all solutions with new codeTemplates field
            Query query = new Query(Criteria.where("codeTemplates").exists(true));
            List<org.bson.Document> newFormatSolutions = mongoTemplate.find(
                query, 
                org.bson.Document.class, 
                "solutions"
            );

            totalSolutions = newFormatSolutions.size();
            System.out.println("📊 Found " + totalSolutions + " solutions with new format");

            for (org.bson.Document doc : newFormatSolutions) {
                try {
                    String solutionId = doc.getObjectId("_id").toString();
                    Object codeTemplatesObj = doc.get("codeTemplates");

                    if (codeTemplatesObj instanceof org.bson.Document) {
                        org.bson.Document codeTemplates = (org.bson.Document) codeTemplatesObj;
                        
                        // Get first language and first code
                        if (!codeTemplates.isEmpty()) {
                            String firstLang = codeTemplates.keySet().iterator().next();
                            Object codesObj = codeTemplates.get(firstLang);
                            
                            if (codesObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> codes = (List<String>) codesObj;
                                
                                if (!codes.isEmpty()) {
                                    String firstCode = codes.get(0);
                                    
                                    // Create old format codeSnippet
                                    Map<String, String> codeSnippet = new HashMap<>();
                                    codeSnippet.put("language", firstLang);
                                    codeSnippet.put("code", firstCode);
                                    codeSnippet.put("description", ""); // Empty description

                                    // Update the document
                                    Query updateQuery = new Query(Criteria.where("_id").is(doc.getObjectId("_id")));
                                    Update update = new Update()
                                        .set("codeSnippet", codeSnippet)
                                        .unset("codeTemplates")  // Remove new field
                                        .set("updatedAt", LocalDateTime.now());

                                    mongoTemplate.updateFirst(updateQuery, update, "solutions");
                                    
                                    System.out.println("✅ Rolled back solution " + solutionId);
                                    rolledBackCount++;
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error rolling back solution: " + e.getMessage();
                    errors.add(errorMsg);
                    System.err.println("❌ " + errorMsg);
                }
            }

            // Summary
            result.put("success", true);
            result.put("totalSolutions", totalSolutions);
            result.put("rolledBackCount", rolledBackCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);
            result.put("message", String.format(
                "Rollback completed: %d rolled back, %d errors",
                rolledBackCount, errorCount
            ));

            System.out.println("\n📈 Rollback Summary:");
            System.out.println("   Total: " + totalSolutions);
            System.out.println("   ✅ Rolled back: " + rolledBackCount);
            System.out.println("   ❌ Errors: " + errorCount);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Rollback failed: " + e.getMessage());
            System.err.println("❌ Rollback failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get migration status - check how many solutions need migration
     */
    public Map<String, Object> getMigrationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            long totalSolutions = solutionRepository.count();
            
            Query oldFormatQuery = new Query(Criteria.where("codeSnippet").exists(true));
            long oldFormatCount = mongoTemplate.count(oldFormatQuery, "solutions");
            
            Query newFormatQuery = new Query(Criteria.where("codeTemplates").exists(true));
            long newFormatCount = mongoTemplate.count(newFormatQuery, "solutions");
            
            Query noCodeQuery = new Query()
                .addCriteria(Criteria.where("codeSnippet").exists(false))
                .addCriteria(Criteria.where("codeTemplates").exists(false));
            long noCodeCount = mongoTemplate.count(noCodeQuery, "solutions");
            
            status.put("totalSolutions", totalSolutions);
            status.put("oldFormat", oldFormatCount);
            status.put("newFormat", newFormatCount);
            status.put("noCode", noCodeCount);
            status.put("needsMigration", oldFormatCount > 0);
            status.put("migrationComplete", oldFormatCount == 0 && newFormatCount > 0);
            
        } catch (Exception e) {
            status.put("error", "Failed to get status: " + e.getMessage());
        }
        
        return status;
    }
}