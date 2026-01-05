// // src/main/java/com/algoarena/service/migration/ApproachMigrationService.java
// package com.algoarena.service.migration;

// import com.algoarena.model.UserApproaches;
// import com.algoarena.model.UserApproaches.ApproachStatus;
// import com.algoarena.repository.UserApproachesRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Service
// public class ApproachMigrationService {

//     @Autowired
//     private UserApproachesRepository userApproachesRepository;

//     /**
//      * Initialize status field for existing approaches
//      * Sets ACCEPTED as default for existing approaches without status
//      * 
//      * POST http://localhost:8080/admin/migrations/approaches/init-status
//      */
//     public Map<String, Object> initializeStatus() {
//         List<UserApproaches> allUserApproaches = userApproachesRepository.findAll();
        
//         int usersUpdated = 0;
//         int approachesUpdated = 0;
        
//         for (UserApproaches userApproaches : allUserApproaches) {
//             boolean needsUpdate = false;
            
//             for (List<UserApproaches.ApproachData> approaches : userApproaches.getApproaches().values()) {
//                 for (UserApproaches.ApproachData approach : approaches) {
//                     if (approach.getStatus() == null) {
//                         approach.setStatus(ApproachStatus.ACCEPTED);
//                         needsUpdate = true;
//                         approachesUpdated++;
//                     }
//                 }
//             }
            
//             if (needsUpdate) {
//                 userApproachesRepository.save(userApproaches);
//                 usersUpdated++;
//             }
//         }
        
//         Map<String, Object> result = new HashMap<>();
//         result.put("success", true);
//         result.put("totalUsers", allUserApproaches.size());
//         result.put("usersUpdated", usersUpdated);
//         result.put("approachesUpdated", approachesUpdated);
//         result.put("message", "Initialized status field for " + approachesUpdated + " approaches across " + usersUpdated + " users");
//         result.put("defaultStatus", "ACCEPTED");
        
//         return result;
//     }
// }