// // src/main/java/com/algoarena/controller/admin/SubmissionTrackingMigrationController.java

// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.SubmissionTrackingMigrationService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations/submissions")
// @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
// public class SubmissionTrackingMigrationController {

//     private static final Logger logger = LoggerFactory.getLogger(SubmissionTrackingMigrationController.class);

//     @Autowired
//     private SubmissionTrackingMigrationService migrationService;

//     /**
//      * ✅ Migrate submission dates from DateTime to LocalDate (UTC)
//      * POST /api/admin/migrations/submissions/fix-dates
//      */
//     @PostMapping("/fix-dates")
//     public ResponseEntity<Map<String, Object>> fixSubmissionDates() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Submission Date Migration CALLED!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.fixSubmissionDates();
        
//         if ((boolean) result.get("success")) {
//             return ResponseEntity.ok(result);
//         } else {
//             return ResponseEntity.status(500).body(result);
//         }
//     }

//     /**
//      * ✅ Get submission tracking statistics (preview)
//      * GET /api/admin/migrations/submissions/stats
//      */
//     @GetMapping("/stats")
//     public ResponseEntity<Map<String, Object>> getSubmissionStats() {
//         logger.info("📊 Getting submission tracking statistics...");
        
//         Map<String, Object> stats = migrationService.getSubmissionStats();
//         return ResponseEntity.ok(stats);
//     }
// }
 