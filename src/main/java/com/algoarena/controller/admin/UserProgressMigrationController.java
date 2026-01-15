// // src/main/java/com/algoarena/controller/admin/UserProgressMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.UserProgressMigrationService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class UserProgressMigrationController {

//     @Autowired
//     private UserProgressMigrationService userProgressMigrationService;

//     /**
//      * Remove version field from all UserProgress documents
//      * POST /api/admin/migrations/user-progress/remove-version
//      */
//     @PostMapping("/user-progress/remove-version")
//     public ResponseEntity<Map<String, Object>> removeVersionField() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 UserProgress version removal CALLED!");
//         System.out.println("🔍 Time: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");

//         Map<String, Object> result = userProgressMigrationService.removeVersionField();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Verify migration completed successfully
//      * GET /api/admin/migrations/user-progress/verify
//      */
//     @GetMapping("/user-progress/verify")
//     public ResponseEntity<Map<String, Object>> verifyMigration() {
//         System.out.println("🔍 Verifying UserProgress migration...");
        
//         Map<String, Object> result = userProgressMigrationService.verifyMigration();
//         return ResponseEntity.ok(result);
//     }
// }