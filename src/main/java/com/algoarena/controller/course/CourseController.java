// src/main/java/com/algoarena/controller/course/CourseController.java
package com.algoarena.controller.course;

import com.algoarena.dto.course.*;
import com.algoarena.model.User;
import com.algoarena.service.course.CourseDocService;
import com.algoarena.service.course.CourseReadProgressService;
import com.algoarena.service.course.CourseTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseTopicService topicService;

    @Autowired
    private CourseDocService docService;

    @Autowired
    private CourseReadProgressService readProgressService;

    // Helper method to check if user is admin
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get course statistics
     * GET /api/courses/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCourseStats() {
        try {
            CourseTopicService.TopicStatsDTO stats = topicService.getTopicStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get PUBLIC topic names only (for dropdowns, navigation)
     * GET /api/courses/topicsnames
     */
    @GetMapping("/topicsnames")
    public ResponseEntity<Map<String, Object>> getPublicTopicNames() {
        try {
            List<CourseTopicNameDTO> topicNames = topicService.getPublicTopicNames();

            List<CourseTopicNamePublicDTO> publicData = topicNames.stream()
                    .map(CourseTopicNamePublicDTO::fromFull)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", publicData);
            response.put("count", publicData.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch topic names");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get ALL topic names (admin only, for management)
     * GET /api/courses/topicsnamesall
     */
    @GetMapping("/topicsnamesall")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> getAllTopicNames() {
        try {
            List<CourseTopicNameDTO> topicNames = topicService.getAllTopicNames();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", topicNames);
            response.put("count", topicNames.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch topic names");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get documents by topic (WITHOUT content)
     * GET /api/courses/topics/{topicId}/docs
     */
    @GetMapping("/topics/{topicId}/docs")
    public ResponseEntity<Map<String, Object>> getDocsByTopic(
            @PathVariable String topicId,
            Authentication authentication) {
        try {
            CourseTopicDTO topic = topicService.getTopicById(topicId);
            List<CourseDocDTO> docs = docService.getDocsByTopic(topicId);
            boolean isAdmin = isAdmin(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (isAdmin) {
                response.put("topic", topic);
                response.put("docs", docs);
            } else {
                response.put("topic", CourseTopicPublicDTO.fromFull(topic));
                List<CourseDocPublicDTO> publicDocs = docs.stream()
                        .map(CourseDocPublicDTO::fromFull)
                        .collect(Collectors.toList());
                response.put("docs", publicDocs);
            }

            response.put("count", docs.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get single document WITH content
     * GET /api/courses/docs/{docId}
     */
    @GetMapping("/docs/{docId}")
    public ResponseEntity<Map<String, Object>> getDocById(
            @PathVariable String docId,
            Authentication authentication) {
        try {
            CourseDocDTO doc = docService.getDocById(docId);
            boolean isAdmin = isAdmin(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (isAdmin) {
                response.put("data", doc);
            } else {
                response.put("data", CourseDocDetailPublicDTO.fromFull(doc));
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Document not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Create new topic
     * POST /api/courses/topics
     */
    @PostMapping("/topics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> createTopic(
            @Valid @RequestBody CourseTopicDTO topicDTO,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CourseTopicDTO createdTopic = topicService.createTopic(topicDTO, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdTopic);
            response.put("message", "Topic created successfully");

            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Update topic
     * PUT /api/courses/topics/{topicId}
     */
    @PutMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> updateTopic(
            @PathVariable String topicId,
            @Valid @RequestBody CourseTopicDTO topicDTO,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CourseTopicDTO updatedTopic = topicService.updateTopic(topicId, topicDTO, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedTopic);
            response.put("message", "Topic updated successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Update topic video links only
     * PUT /api/courses/topics/{topicId}/videos
     */
    @PutMapping("/topics/{topicId}/videos")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> updateTopicVideos(
            @PathVariable String topicId,
            @Valid @RequestBody UpdateVideoLinksRequest request) {
        try {
            CourseTopicDTO updatedTopic = topicService.updateTopicVideos(topicId, request.getVideoLinks());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedTopic);
            response.put("message", "Video links updated successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update video links");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update video links");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Toggle topic public/private
     * PUT /api/courses/topics/{topicId}/visibility
     */
    @PutMapping("/topics/{topicId}/visibility")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> toggleTopicVisibility(@PathVariable String topicId) {
        try {
            CourseTopicDTO updatedTopic = topicService.toggleTopicVisibility(topicId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedTopic);
            response.put("message", "Topic visibility toggled successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to toggle visibility");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to toggle visibility");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Delete topic
     * DELETE /api/courses/topics/{topicId}
     */
    @DeleteMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTopic(@PathVariable String topicId) {
        try {
            topicService.deleteTopic(topicId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Topic deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete topic");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Create new document
     * POST /api/courses/docs
     */
    @PostMapping("/docs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> createDoc(
            @Valid @RequestBody CourseDocDTO docDTO,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CourseDocDTO createdDoc = docService.createDoc(docDTO, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdDoc);
            response.put("message", "Document created successfully");

            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Update document
     * PUT /api/courses/docs/{docId}
     */
    @PutMapping("/docs/{docId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> updateDoc(
            @PathVariable String docId,
            @Valid @RequestBody CourseDocDTO docDTO,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CourseDocDTO updatedDoc = docService.updateDoc(docId, docDTO, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedDoc);
            response.put("message", "Document updated successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Delete document
     * DELETE /api/courses/docs/{docId}
     */
    @DeleteMapping("/docs/{docId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> deleteDoc(@PathVariable String docId) {
        try {
            docService.deleteDoc(docId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Move document to a different topic
     * PUT /api/courses/docs/{docId}/move
     */
    @PutMapping("/docs/{docId}/move")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> moveDocToTopic(
            @PathVariable String docId,
            @Valid @RequestBody MoveDocRequest request) {
        try {
            CourseDocDTO movedDoc = docService.moveDocToTopic(docId, request.getNewTopicId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movedDoc);
            response.put("message", "Document moved successfully to new topic");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to move document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to move document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get user's read statistics
     * GET /api/courses/read/stats
     */
    @GetMapping("/read/stats")
    public ResponseEntity<Map<String, Object>> getUserReadStats(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CourseReadStatsDTO stats = readProgressService.getUserReadStats(currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch read statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Toggle document read/unread status
     * PUT /api/courses/docs/{docId}/read
     */
    @PutMapping("/docs/{docId}/read")
    public ResponseEntity<Map<String, Object>> toggleDocReadStatus(
            @PathVariable String docId,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            readProgressService.toggleDocReadStatus(currentUser.getId(), docId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document read status toggled successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to toggle read status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to toggle read status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}