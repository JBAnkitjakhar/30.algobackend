// File: src/main/java/com/algoarena/controller/dsa/CategoryController.java

package com.algoarena.controller.dsa;

import com.algoarena.dto.dsa.CategoryDTO;
import com.algoarena.dto.dsa.CategoryMetadataDTO;
import com.algoarena.dto.dsa.CategoryMetadataPublicDTO;
import com.algoarena.dto.dsa.CategoryPublicDTO;
import com.algoarena.model.User;
import com.algoarena.service.dsa.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // Helper method to check if user is admin
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    /**
     * GET /api/categories
     * Get all categories with question IDs
     * Admin: Returns full CategoryDTO
     * User: Returns CategoryPublicDTO (without creator info & timestamps)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllCategories(Authentication authentication) {
        Map<String, CategoryDTO> categories = categoryService.getAllCategories();
        
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(categories);
        } else {
            Map<String, CategoryPublicDTO> publicCategories = new LinkedHashMap<>();
            categories.forEach((key, value) -> 
                publicCategories.put(key, CategoryPublicDTO.fromFull(value))
            );
            return ResponseEntity.ok(publicCategories);
        }
    }

    /**
     * GET /api/categories/metadata
     * Get category metadata for dropdowns
     * Admin: Returns full metadata (with creator info & timestamps)
     * User: Returns public metadata (without creator info & timestamps)
     */
    @GetMapping("/metadata")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCategoriesMetadata(Authentication authentication) {
        List<CategoryMetadataDTO> metadata = categoryService.getCategoriesMetadata();
        
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(metadata);
        } else {
            List<CategoryMetadataPublicDTO> publicMetadata = metadata.stream()
                    .map(CategoryMetadataPublicDTO::fromFull)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(publicMetadata);
        }
    }

    /**
     * GET /api/categories/{id}
     * Get single category by ID
     * Admin: Returns full CategoryDTO
     * User: Returns CategoryPublicDTO (without creator info & timestamps)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCategoryById(
            @PathVariable String id,
            Authentication authentication) {
        try {
            CategoryDTO category = categoryService.getCategoryById(id);
            
            if (isAdmin(authentication)) {
                return ResponseEntity.ok(category);
            } else {
                return ResponseEntity.ok(CategoryPublicDTO.fromFull(category));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/categories
     * Create new category (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> createCategory(
            @Valid @RequestBody CategoryDTO categoryDTO,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            CategoryDTO createdCategory = categoryService.createCategory(categoryDTO, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category created successfully");
            response.put("data", createdCategory);
            
            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create category");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * PUT /api/categories/{id}
     * Update category name and/or displayOrder (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category updated successfully");
            response.put("data", updatedCategory);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to update category");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * DELETE /api/categories/{id}
     * Delete category and all its questions (cascade) (SuperAdmin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable String id) {
        try {
            Map<String, Object> result = categoryService.deleteCategory(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete category");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}