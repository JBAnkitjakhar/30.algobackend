// src/main/java/com/algoarena/service/dsa/QuestionService.java
package com.algoarena.service.dsa;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import com.algoarena.dto.dsa.AdminQuestionSummaryDTO;
import com.algoarena.dto.dsa.QuestionDTO;
import com.algoarena.dto.user.QuestionsMetadataDTO;
import com.algoarena.model.QuestionLevel;
import com.algoarena.model.Solution;
import com.algoarena.model.Question;
import com.algoarena.model.User;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.repository.CategoryRepository;
import com.algoarena.repository.SolutionRepository;
import com.algoarena.service.file.CloudinaryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private SolutionService solutionService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ApproachService approachService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @CacheEvict(value = { "globalCategories", "adminQuestionsSummary", "questionsMetadata" }, allEntries = true)
    @Transactional
    public QuestionDTO createQuestion(QuestionDTO questionDTO, User currentUser) {
        if (questionRepository.existsByTitleIgnoreCase(questionDTO.getTitle())) {
            throw new RuntimeException("Question with this title already exists");
        }

        if (!categoryRepository.existsById(questionDTO.getCategoryId())) {
            throw new RuntimeException("Category not found with id: " + questionDTO.getCategoryId());
        }

        Question question = new Question();
        question.setTitle(questionDTO.getTitle());
        question.setStatement(questionDTO.getStatement());
        question.setImageUrls(questionDTO.getImageUrls());

        // Set code templates
        question.setUserStarterCode(questionDTO.getUserStarterCode());
        question.setSubmitTemplate(questionDTO.getSubmitTemplate());
        question.setRunTemplate(questionDTO.getRunTemplate());
        question.setMethodName(questionDTO.getMethodName()); // ✅ NEW

        // Set testcases
        if (questionDTO.getTestcases() != null) {
            List<Question.Testcase> testcases = questionDTO.getTestcases().stream()
                    .map(dto -> new Question.Testcase(
                            dto.getId(),
                            dto.getInput(),
                            dto.getExpectedOutput()))
                    .toList();
            question.setTestcases(testcases);
        }

        question.setCategoryId(questionDTO.getCategoryId());
        question.setLevel(questionDTO.getLevel());

        if (questionDTO.getDisplayOrder() != null) {
            question.setDisplayOrder(questionDTO.getDisplayOrder());
        } else {
            Integer maxOrder = questionRepository.findTop1ByCategoryIdAndLevelOrderByDisplayOrderDesc(
                    questionDTO.getCategoryId(),
                    questionDTO.getLevel())
                    .map(Question::getDisplayOrder)
                    .orElse(0);
            question.setDisplayOrder(maxOrder + 1);
        }

        question.setCreatedById(currentUser.getId());
        question.setCreatedByName(currentUser.getName());
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());

        Question savedQuestion = questionRepository.save(question);

        categoryService.addQuestionToCategory(
                savedQuestion.getCategoryId(),
                savedQuestion.getId(),
                savedQuestion.getLevel());

        return QuestionDTO.fromEntity(savedQuestion);
    }

    @CacheEvict(value = { "globalCategories", "adminQuestionsSummary", "questionsMetadata",
            "questionDetail" }, allEntries = true)
    @Transactional
    public QuestionDTO updateQuestion(String id, QuestionDTO questionDTO) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        if (!question.getTitle().equals(questionDTO.getTitle()) &&
                questionRepository.existsByTitleIgnoreCase(questionDTO.getTitle())) {
            throw new RuntimeException("Question with this title already exists");
        }

        if (!question.getCategoryId().equals(questionDTO.getCategoryId())) {
            if (!categoryRepository.existsById(questionDTO.getCategoryId())) {
                throw new RuntimeException("Category not found with id: " + questionDTO.getCategoryId());
            }
        }

        if (questionDTO.getVersion() != null && !questionDTO.getVersion().equals(question.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Question was modified by another user. Please refresh and try again.");
        }

        String oldCategoryId = question.getCategoryId();
        QuestionLevel oldLevel = question.getLevel();

        question.setTitle(questionDTO.getTitle());
        question.setStatement(questionDTO.getStatement());
        question.setImageUrls(questionDTO.getImageUrls());

        // Update code templates
        question.setUserStarterCode(questionDTO.getUserStarterCode());
        question.setSubmitTemplate(questionDTO.getSubmitTemplate());
        question.setRunTemplate(questionDTO.getRunTemplate());
        question.setMethodName(questionDTO.getMethodName()); // ✅ NEW

        // Update testcases
        if (questionDTO.getTestcases() != null) {
            List<Question.Testcase> testcases = questionDTO.getTestcases().stream()
                    .map(dto -> new Question.Testcase(
                            dto.getId(),
                            dto.getInput(),
                            dto.getExpectedOutput()))
                    .toList();
            question.setTestcases(testcases);
        }

        boolean categoryChanged = !oldCategoryId.equals(questionDTO.getCategoryId());
        boolean levelChanged = oldLevel != questionDTO.getLevel();

        if (categoryChanged || levelChanged) {
            question.setCategoryId(questionDTO.getCategoryId());
            question.setLevel(questionDTO.getLevel());

            categoryService.moveQuestion(
                    oldCategoryId,
                    question.getCategoryId(),
                    question.getId(),
                    oldLevel,
                    question.getLevel());
        }

        if (questionDTO.getDisplayOrder() != null) {
            question.setDisplayOrder(questionDTO.getDisplayOrder());
        }

        question.setUpdatedAt(LocalDateTime.now());

        Question updatedQuestion = questionRepository.save(question);

        return QuestionDTO.fromEntity(updatedQuestion);
    }

    @CacheEvict(value = { "globalCategories", "adminQuestionsSummary", "questionsMetadata",
            "questionDetail", "userMeStats", "adminSolutionsSummary", "solutionDetail",
            "questionSolutions" }, allEntries = true)
    @Transactional
    public void deleteQuestion(String id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        // STEP 1: Delete all solutions
        List<Solution> solutions = solutionRepository.findByQuestionId(id);
        for (Solution solution : solutions) {
            solutionService.deleteSolution(solution.getId());
        }

        // STEP 2: Delete question's own images
        if (question.getImageUrls() != null && !question.getImageUrls().isEmpty()) {
            for (String imageUrl : question.getImageUrls()) {
                try {
                    String publicId = extractPublicIdFromUrl(imageUrl);
                    cloudinaryService.deleteImage(publicId);
                } catch (Exception e) {
                    System.err.println("Failed to delete image: " + e.getMessage());
                }
            }
        }

        // STEP 3: Remove from category
        categoryService.removeQuestionFromCategory(
                question.getCategoryId(),
                question.getId(),
                question.getLevel());

        // STEP 4: Delete approaches
        approachService.deleteAllApproachesForQuestion(id);

        // STEP 5: Delete question from database
        questionRepository.deleteById(id);
    }

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

            if (afterUpload.startsWith("v") && afterUpload.indexOf("/") > 0) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

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
        return questionRepository.existsById(id);
    }

    public boolean existsByTitle(String title) {
        return questionRepository.existsByTitleIgnoreCase(title);
    }

    @Cacheable(value = "adminQuestionsSummary", key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<AdminQuestionSummaryDTO> getAdminQuestionsSummary(Pageable pageable) {
        Page<Question> questions = questionRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<String> questionIds = questions.getContent().stream()
                .map(Question::getId)
                .toList();

        Map<String, Integer> solutionCounts = getSolutionCountsForQuestions(questionIds);

        return questions.map(question -> {
            AdminQuestionSummaryDTO dto = new AdminQuestionSummaryDTO();

            // Basic info
            dto.setId(question.getId());
            dto.setTitle(question.getTitle());
            dto.setLevel(question.getLevel());
            dto.setCategoryId(question.getCategoryId());
            dto.setDisplayOrder(question.getDisplayOrder());

            // Image count
            dto.setImageCount(question.getImageUrls() != null ? question.getImageUrls().size() : 0);

            // ✅ NEW: Set userStarterCode languages
            if (question.getUserStarterCode() != null) {
                dto.setUserStarterCodeLanguages(new java.util.ArrayList<>(question.getUserStarterCode().keySet()));
            } else {
                dto.setUserStarterCodeLanguages(new java.util.ArrayList<>());
            }

            // ✅ NEW: Set submitTemplate languages
            if (question.getSubmitTemplate() != null) {
                dto.setSubmitTemplateLanguages(new java.util.ArrayList<>(question.getSubmitTemplate().keySet()));
            } else {
                dto.setSubmitTemplateLanguages(new java.util.ArrayList<>());
            }

            // ✅ RENAMED: Set runTemplate languages
            if (question.getRunTemplate() != null) {
                dto.setRunTemplateLanguages(new java.util.ArrayList<>(question.getRunTemplate().keySet()));
            } else {
                dto.setRunTemplateLanguages(new java.util.ArrayList<>());
            }

            // ✅ NEW: Set testcase count
            if (question.getTestcases() != null) {
                dto.setTestcaseCount(question.getTestcases().size());
            } else {
                dto.setTestcaseCount(0);
            }

            // Metadata
            dto.setCreatedByName(question.getCreatedByName());
            dto.setUpdatedAt(question.getUpdatedAt());
            dto.setSolutionCount(solutionCounts.getOrDefault(question.getId(), 0));

            return dto;
        });
    }

    private Map<String, Integer> getSolutionCountsForQuestions(List<String> questionIds) {
        if (questionIds.isEmpty()) {
            return new HashMap<>();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("questionId").in(questionIds)),
                Aggregation.group("questionId").count().as("count"));

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(
                aggregation,
                "solutions",
                org.bson.Document.class);

        Map<String, Integer> counts = new HashMap<>();
        for (org.bson.Document doc : results.getMappedResults()) {
            String questionId = doc.getString("_id");
            Integer count = doc.getInteger("count");
            counts.put(questionId, count);
        }

        return counts;
    }

    @Cacheable(value = "questionDetail", key = "#questionId")
    public QuestionDTO getQuestionById(String questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

        return QuestionDTO.fromEntity(question);
    }

    @Cacheable(value = "questionsMetadata")
    public QuestionsMetadataDTO getQuestionsMetadata() {
        List<Question> allQuestions = questionRepository.findAll();
        Map<String, QuestionsMetadataDTO.QuestionMetadata> metadataMap = new HashMap<>();

        for (Question question : allQuestions) {
            QuestionsMetadataDTO.QuestionMetadata metadata = new QuestionsMetadataDTO.QuestionMetadata(
                    question.getId(),
                    question.getTitle(),
                    question.getLevel(),
                    question.getCategoryId());
            metadataMap.put(question.getId(), metadata);
        }

        QuestionsMetadataDTO result = new QuestionsMetadataDTO();
        result.setQuestions(metadataMap);

        return result;
    }
}