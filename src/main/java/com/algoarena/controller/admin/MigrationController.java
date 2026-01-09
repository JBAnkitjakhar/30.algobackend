// // src/main/java/com/algoarena/controller/admin/MigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.SolutionMigrationService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class MigrationController {

//     @Autowired
//     private SolutionMigrationService solutionMigrationService;

//     /**
//      * Migrate solutions from old CodeSnippet format to new codeTemplates Map format
//      * 
//      * POST http://localhost:8080/api/admin/migrations/solutions/migrate
//      */
//     @PostMapping("/solutions/migrate")
//     public ResponseEntity<Map<String, Object>> migrateSolutions() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 Solution migration endpoint CALLED!");
//         System.out.println("🔍 Request received at: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");
        
//         Map<String, Object> result = solutionMigrationService.migrateSolutionsToNewFormat();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Reorder codeTemplates field to correct position for already migrated solutions
//      * 
//      * POST http://localhost:8080/api/admin/migrations/solutions/reorder-fields
//      */
//     @PostMapping("/solutions/reorder-fields")
//     public ResponseEntity<Map<String, Object>> reorderSolutionFields() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 Solution field reordering endpoint CALLED!");
//         System.out.println("🔍 Request received at: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");
        
//         Map<String, Object> result = solutionMigrationService.reorderCodeTemplatesField();
//         return ResponseEntity.ok(result);
//     }
// }