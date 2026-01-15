// // src/main/java/com/algoarena/controller/admin/UserApproachesMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.UserApproachesMigrationService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class UserApproachesMigrationController {

//     @Autowired
//     private UserApproachesMigrationService userApproachesMigrationService;

//     /**
//      * Remove version field from all UserApproaches documents
//      * POST /api/admin/migrations/user-approaches/remove-version
//      */
//     @PostMapping("/user-approaches/remove-version")
//     public ResponseEntity<Map<String, Object>> removeVersionField() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 UserApproaches version removal CALLED!");
//         System.out.println("🔍 Time: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");

//         Map<String, Object> result = userApproachesMigrationService.removeVersionField();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Verify migration completed successfully
//      * GET /api/admin/migrations/user-approaches/verify
//      */
//     @GetMapping("/user-approaches/verify")
//     public ResponseEntity<Map<String, Object>> verifyMigration() {
//         System.out.println("🔍 Verifying UserApproaches migration...");
        
//         Map<String, Object> result = userApproachesMigrationService.verifyMigration();
//         return ResponseEntity.ok(result);
//     }
// }