// // src/main/java/com/algoarena/controller/admin/MigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.ApproachMigrationService;
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
//     private ApproachMigrationService approachMigrationService;

//     @PostMapping("/approaches/add-complexity-description")
//     public ResponseEntity<Map<String, Object>> addComplexityDescription() {
//         System.out.println("🔍 ========================================");
//         System.out.println("🔍 Approach complexity description migration endpoint CALLED!");
//         System.out.println("🔍 Request received at: " + java.time.LocalDateTime.now());
//         System.out.println("🔍 ========================================");
        
//         Map<String, Object> result = approachMigrationService.addComplexityDescriptionField();
//         return ResponseEntity.ok(result);
//     }
// }