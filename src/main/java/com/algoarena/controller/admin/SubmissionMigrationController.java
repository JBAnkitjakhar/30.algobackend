// // src/main/java/com/algoarena/controller/admin/SubmissionMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.SubmissionHistoryMigrationService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class SubmissionMigrationController {

//     @Autowired
//     private SubmissionHistoryMigrationService submissionHistoryMigrationService;

//     /**
//      * Migrate all approaches to submission history
//      * POST /admin/migrations/submission-history
//      */
//     @PostMapping("/submission-history")
//     public ResponseEntity<Map<String, Object>> migrateSubmissionHistory() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 Submission history migration CALLED!");
//         System.out.println("🔍 Time: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");

//         Map<String, Object> result = submissionHistoryMigrationService.migrateFromApproaches();
//         return ResponseEntity.ok(result);
//     }
// }