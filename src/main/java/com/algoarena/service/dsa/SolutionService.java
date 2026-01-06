// src/main/java/com/algoarena/service/dsa/SolutionService.java
package com.algoarena.service.dsa;

import com.algoarena.dto.dsa.AdminSolutionSummaryDTO;
import com.algoarena.dto.dsa.SolutionDTO;
import com.algoarena.model.Solution;
import com.algoarena.model.User;
import com.algoarena.repository.SolutionRepository;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.service.file.CloudinaryService;
import com.algoarena.service.file.VisualizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SolutionService {

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private VisualizerService visualizerService;

    /**
     * Get solution by ID - CACHED
     */
    @Cacheable(value = "solutionDetail", key = "#id")
    public SolutionDTO getSolutionById(String id) {
        Solution solution = solutionRepository.findById(id).orElse(null);
        return solution != null ? SolutionDTO.fromEntity(solution) : null;
    }

    /**
     * Get solutions by question - CACHED
     */
    @Cacheable(value = "questionSolutions", key = "#questionId")
    public List<SolutionDTO> getSolutionsByQuestion(String questionId) {
        List<Solution> solutions = solutionRepository.findByQuestionIdOrderByCreatedAtAsc(questionId);
        return solutions.stream()
                .map(SolutionDTO::fromEntity)
                .toList();
    }

    /**
     * Create new solution
     */
    @CacheEvict(value = {
            "adminSolutionsSummary",
            "questionSolutions",
            "adminQuestionsSummary"
    }, allEntries = true)
    public SolutionDTO createSolution(String questionId, SolutionDTO solutionDTO, User createdBy) {
        // Verify question exists
        if (!questionRepository.existsById(questionId)) {
            throw new RuntimeException("Question not found");
        }

        Solution solution = new Solution();

        solution.setQuestionId(questionId);
        solution.setCreatedByName(createdBy.getName());

        solution.setContent(solutionDTO.getContent());
        solution.setDriveLink(validateAndCleanDriveLink(solutionDTO.getDriveLink()));
        solution.setYoutubeLink(validateAndCleanYoutubeLink(solutionDTO.getYoutubeLink()));
        solution.setImageUrls(solutionDTO.getImageUrls());
        solution.setVisualizerFileIds(solutionDTO.getVisualizerFileIds());

        // ✅ NEW: Set code templates map
        solution.setCodeTemplates(solutionDTO.getCodeTemplates());

        Solution savedSolution = solutionRepository.save(solution);

        return SolutionDTO.fromEntity(savedSolution);
    }

    /**
     * Update solution
     */
    @CacheEvict(value = {
            "adminSolutionsSummary",
            "solutionDetail",
            "questionSolutions"
    }, allEntries = true)
    public SolutionDTO updateSolution(String id, SolutionDTO solutionDTO) {
        Solution solution = solutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        solution.setContent(solutionDTO.getContent());
        solution.setDriveLink(validateAndCleanDriveLink(solutionDTO.getDriveLink()));
        solution.setYoutubeLink(validateAndCleanYoutubeLink(solutionDTO.getYoutubeLink()));
        solution.setImageUrls(solutionDTO.getImageUrls());
        solution.setVisualizerFileIds(solutionDTO.getVisualizerFileIds());

        // ✅ NEW: Update code templates map
        solution.setCodeTemplates(solutionDTO.getCodeTemplates());

        Solution updatedSolution = solutionRepository.save(solution);

        return SolutionDTO.fromEntity(updatedSolution);
    }

    /**
     * Delete solution
     */
    @CacheEvict(value = {
            "adminSolutionsSummary",
            "solutionDetail",
            "questionSolutions",
            "adminQuestionsSummary"
    }, allEntries = true)
    public void deleteSolution(String id) {
        Solution solution = solutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        // Delete Cloudinary images
        if (solution.getImageUrls() != null && !solution.getImageUrls().isEmpty()) {
            for (String imageUrl : solution.getImageUrls()) {
                try {
                    String publicId = extractPublicIdFromUrl(imageUrl);
                    cloudinaryService.deleteImage(publicId);
                } catch (Exception e) {
                    System.err.println("  ✗ Failed to delete image: " + e.getMessage());
                }
            }
        }

        // Delete visualizer files
        try {
            visualizerService.deleteAllVisualizersForSolution(id);
        } catch (Exception e) {
            System.err.println("Failed to clean up visualizer files: " + e.getMessage());
        }

        // Delete solution from database
        solutionRepository.deleteById(id);
    }

    /**
     * Helper method to extract Cloudinary public ID from URL
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            throw new IllegalArgumentException("Invalid Cloudinary URL");
        }

        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                throw new IllegalArgumentException("Invalid Cloudinary URL format");
            }

            String afterUpload = imageUrl.substring(uploadIndex + 8);

            // Remove version prefix (e.g., "v1234567890/")
            if (afterUpload.startsWith("v") && afterUpload.indexOf("/") > 0) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex > 0) {
                return afterUpload.substring(0, dotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract public ID: " + e.getMessage());
        }
    }

    public boolean existsById(String id) {
        return solutionRepository.existsById(id);
    }

    public long countSolutionsByQuestion(String questionId) {
        return solutionRepository.countByQuestionId(questionId);
    }

    public List<SolutionDTO> getSolutionsWithVisualizers() {
        return solutionRepository.findSolutionsWithVisualizers().stream()
                .map(SolutionDTO::fromEntity)
                .toList();
    }

    public List<SolutionDTO> getSolutionsWithImages() {
        return solutionRepository.findSolutionsWithImages().stream()
                .map(SolutionDTO::fromEntity)
                .toList();
    }

    /**
     * Add/remove image/visualizer methods
     */
    @CacheEvict(value = { "adminSolutionsSummary", "solutionDetail", "questionSolutions" }, allEntries = true)
    public SolutionDTO addImageToSolution(String solutionId, String imageUrl) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        if (solution.getImageUrls() == null) {
            solution.setImageUrls(List.of(imageUrl));
        } else {
            if (solution.getImageUrls().size() >= 10) {
                throw new RuntimeException("Maximum 10 images per solution");
            }
            var updatedUrls = new java.util.ArrayList<>(solution.getImageUrls());
            updatedUrls.add(imageUrl);
            solution.setImageUrls(updatedUrls);
        }

        return SolutionDTO.fromEntity(solutionRepository.save(solution));
    }

    @CacheEvict(value = { "adminSolutionsSummary", "solutionDetail", "questionSolutions" }, allEntries = true)
    public SolutionDTO removeImageFromSolution(String solutionId, String imageUrl) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        if (solution.getImageUrls() != null) {
            var updatedUrls = new java.util.ArrayList<>(solution.getImageUrls());
            updatedUrls.remove(imageUrl);
            solution.setImageUrls(updatedUrls.isEmpty() ? null : updatedUrls);
        }

        return SolutionDTO.fromEntity(solutionRepository.save(solution));
    }

    @CacheEvict(value = { "adminSolutionsSummary", "solutionDetail", "questionSolutions" }, allEntries = true)
    public SolutionDTO addVisualizerToSolution(String solutionId, String visualizerFileId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        if (solution.getVisualizerFileIds() == null) {
            solution.setVisualizerFileIds(List.of(visualizerFileId));
        } else {
            if (solution.getVisualizerFileIds().size() >= 2) {
                throw new RuntimeException("Maximum 2 visualizers per solution");
            }
            var updatedFileIds = new java.util.ArrayList<>(solution.getVisualizerFileIds());
            updatedFileIds.add(visualizerFileId);
            solution.setVisualizerFileIds(updatedFileIds);
        }

        return SolutionDTO.fromEntity(solutionRepository.save(solution));
    }

    @CacheEvict(value = { "adminSolutionsSummary", "solutionDetail", "questionSolutions" }, allEntries = true)
    public SolutionDTO removeVisualizerFromSolution(String solutionId, String visualizerFileId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found"));

        if (solution.getVisualizerFileIds() != null) {
            var updatedFileIds = new java.util.ArrayList<>(solution.getVisualizerFileIds());
            updatedFileIds.remove(visualizerFileId);
            solution.setVisualizerFileIds(updatedFileIds.isEmpty() ? null : updatedFileIds);
        }

        return SolutionDTO.fromEntity(solutionRepository.save(solution));
    }

    // Link validation helpers
    private String validateAndCleanDriveLink(String driveLink) {
        if (driveLink == null || driveLink.trim().isEmpty())
            return null;
        String cleanLink = driveLink.trim();
        if (!cleanLink.contains("drive.google.com") && !cleanLink.contains("docs.google.com")) {
            throw new IllegalArgumentException("Invalid Google Drive link");
        }
        if (!cleanLink.startsWith("http://") && !cleanLink.startsWith("https://")) {
            cleanLink = "https://" + cleanLink;
        }
        return cleanLink;
    }

    private String validateAndCleanYoutubeLink(String youtubeLink) {
        if (youtubeLink == null || youtubeLink.trim().isEmpty())
            return null;
        String cleanLink = youtubeLink.trim();
        if (!cleanLink.contains("youtube.com") && !cleanLink.contains("youtu.be")) {
            throw new IllegalArgumentException("Invalid YouTube link");
        }
        if (!cleanLink.startsWith("http://") && !cleanLink.startsWith("https://")) {
            cleanLink = "https://" + cleanLink;
        }
        return cleanLink;
    }

    /**
     * ✅ UPDATED: Now returns language -> count mapping
     */
    @Cacheable(value = "adminSolutionsSummary", key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<AdminSolutionSummaryDTO> getAdminSolutionsSummary(Pageable pageable) {
        Page<Solution> solutions = solutionRepository.findAllByOrderByCreatedAtDesc(pageable);

        return solutions.map(solution -> {
            AdminSolutionSummaryDTO dto = new AdminSolutionSummaryDTO();
            dto.setId(solution.getId());
            dto.setQuestionId(solution.getQuestionId());
            dto.setImageCount(solution.getImageUrls() != null ? solution.getImageUrls().size() : 0);
            dto.setVisualizerCount(
                    solution.getVisualizerFileIds() != null ? solution.getVisualizerFileIds().size() : 0);

            // ✅ NEW: Calculate language -> count mapping
            Map<String, Integer> templateCounts = new HashMap<>();
            if (solution.getCodeTemplates() != null && !solution.getCodeTemplates().isEmpty()) {
                solution.getCodeTemplates().forEach((lang, codes) -> {
                    templateCounts.put(lang, codes != null ? codes.size() : 0);
                });
            }
            dto.setCodeTemplatesCounts(templateCounts);

            dto.setHasYoutubeLink(solution.hasValidYoutubeLink());
            dto.setHasDriveLink(solution.hasValidDriveLink());

            dto.setCreatedByName(solution.getCreatedByName());
            dto.setCreatedAt(solution.getCreatedAt());
            dto.setUpdatedAt(solution.getUpdatedAt());
            return dto;
        });
    }
}