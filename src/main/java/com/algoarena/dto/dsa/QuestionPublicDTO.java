// File: src/main/java/com/algoarena/dto/dsa/QuestionPublicDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.QuestionLevel;
import java.util.List;
import java.util.Map;

/**
 * Public version of QuestionDTO
 * Excludes: version, displayOrder, createdByName, createdById, createdAt, updatedAt
 */
public class QuestionPublicDTO {

    private String id;
    private String title;
    private String statement;
    private List<String> imageUrls;
    private String imageFolderUrl;
    private String categoryId;
    private QuestionLevel level;
    
    // Code templates
    private Map<String, String> userStarterCode;
    private Map<String, String> generalTemplate;
    private Map<String, String> correctSolution;
    
    // Testcases
    private List<TestcaseDTO> testcases;

    // Inner DTO for Testcase
    public static class TestcaseDTO {
        private Integer id;
        private Map<String, Object> input;
        private Object expectedOutput;
        private Long expectedTimeLimit;

        public TestcaseDTO() {}

        public TestcaseDTO(QuestionDTO.TestcaseDTO testcase) {
            this.id = testcase.getId();
            this.input = testcase.getInput();
            this.expectedOutput = testcase.getExpectedOutput();
            this.expectedTimeLimit = testcase.getExpectedTimeLimit();
        }

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> input) { this.input = input; }

        public Object getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(Object expectedOutput) { this.expectedOutput = expectedOutput; }

        public Long getExpectedTimeLimit() { return expectedTimeLimit; }
        public void setExpectedTimeLimit(Long expectedTimeLimit) { this.expectedTimeLimit = expectedTimeLimit; }
    }

    public QuestionPublicDTO() {}

    public static QuestionPublicDTO fromFull(QuestionDTO full) {
        QuestionPublicDTO dto = new QuestionPublicDTO();
        dto.id = full.getId();
        dto.title = full.getTitle();
        dto.statement = full.getStatement();
        dto.imageUrls = full.getImageUrls();
        dto.imageFolderUrl = full.getImageFolderUrl();
        dto.categoryId = full.getCategoryId();
        dto.level = full.getLevel();
        dto.userStarterCode = full.getUserStarterCode();
        dto.generalTemplate = full.getGeneralTemplate();
        dto.correctSolution = full.getCorrectSolution();
        
        if (full.getTestcases() != null) {
            dto.testcases = full.getTestcases().stream()
                    .map(TestcaseDTO::new)
                    .toList();
        }
        
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getImageFolderUrl() { return imageFolderUrl; }
    public void setImageFolderUrl(String imageFolderUrl) { this.imageFolderUrl = imageFolderUrl; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public QuestionLevel getLevel() { return level; }
    public void setLevel(QuestionLevel level) { this.level = level; }

    public Map<String, String> getUserStarterCode() { return userStarterCode; }
    public void setUserStarterCode(Map<String, String> userStarterCode) { this.userStarterCode = userStarterCode; }

    public Map<String, String> getGeneralTemplate() { return generalTemplate; }
    public void setGeneralTemplate(Map<String, String> generalTemplate) { this.generalTemplate = generalTemplate; }

    public Map<String, String> getCorrectSolution() { return correctSolution; }
    public void setCorrectSolution(Map<String, String> correctSolution) { this.correctSolution = correctSolution; }

    public List<TestcaseDTO> getTestcases() { return testcases; }
    public void setTestcases(List<TestcaseDTO> testcases) { this.testcases = testcases; }
}