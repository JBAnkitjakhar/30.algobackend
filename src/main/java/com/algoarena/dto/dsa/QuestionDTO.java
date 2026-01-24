// src/main/java/com/algoarena/dto/dsa/QuestionDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.Question;
import com.algoarena.model.QuestionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QuestionDTO {

    private String id;
    private Long version;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Statement is required")
    @Size(min = 20, max = 15000, message = "Statement must be between 20 and 15000 characters")
    private String statement;

    private List<String> imageUrls;

    @NotNull(message = "Category is required")
    private String categoryId;

    @NotNull(message = "Level is required")
    private QuestionLevel level;

    private Integer displayOrder;

    // Code templates
    private Map<String, String> userStarterCode;
    private Map<String, String> generalTemplate;
    private Map<String, String> correctSolution;

    // Method name for code execution (e.g., "twoSum", "maxDistance")
    @NotBlank(message = "Method name is required")
    private String methodName;

    // Testcases
    private List<TestcaseDTO> testcases;

    // Metadata
    private String createdByName;
    private String createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Inner DTO for Testcase
    public static class TestcaseDTO {
        private Integer id;
        private Map<String, Object> input;
        private Object expectedOutput;

        public TestcaseDTO() {
        }

        public TestcaseDTO(Question.Testcase testcase) {
            this.id = testcase.getId();
            this.input = testcase.getInput();
            this.expectedOutput = testcase.getExpectedOutput();
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Map<String, Object> getInput() {
            return input;
        }

        public void setInput(Map<String, Object> input) {
            this.input = input;
        }

        public Object getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(Object expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }

    // Constructors
    public QuestionDTO() {
    }

    public QuestionDTO(Question question) {
        this.id = question.getId();
        this.version = question.getVersion();
        this.title = question.getTitle();
        this.statement = question.getStatement();
        this.imageUrls = question.getImageUrls();

        this.userStarterCode = question.getUserStarterCode();
        this.generalTemplate = question.getGeneralTemplate();
        this.correctSolution = question.getCorrectSolution();
        this.methodName = question.getMethodName(); // ✅ NEW

        if (question.getTestcases() != null) {
            this.testcases = question.getTestcases().stream()
                    .map(TestcaseDTO::new)
                    .toList();
        }

        this.categoryId = question.getCategoryId();
        this.level = question.getLevel();
        this.displayOrder = question.getDisplayOrder();
        this.createdByName = question.getCreatedByName();
        this.createdById = question.getCreatedById();
        this.createdAt = question.getCreatedAt();
        this.updatedAt = question.getUpdatedAt();
    }

    public static QuestionDTO fromEntity(Question question) {
        return new QuestionDTO(question);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Map<String, String> getUserStarterCode() {
        return userStarterCode;
    }

    public void setUserStarterCode(Map<String, String> userStarterCode) {
        this.userStarterCode = userStarterCode;
    }

    public Map<String, String> getGeneralTemplate() {
        return generalTemplate;
    }

    public void setGeneralTemplate(Map<String, String> generalTemplate) {
        this.generalTemplate = generalTemplate;
    }

    public Map<String, String> getCorrectSolution() {
        return correctSolution;
    }

    public void setCorrectSolution(Map<String, String> correctSolution) {
        this.correctSolution = correctSolution;
    }

    // ✅ NEW: Getter and Setter for methodName
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<TestcaseDTO> getTestcases() {
        return testcases;
    }

    public void setTestcases(List<TestcaseDTO> testcases) {
        this.testcases = testcases;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public QuestionLevel getLevel() {
        return level;
    }

    public void setLevel(QuestionLevel level) {
        this.level = level;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}