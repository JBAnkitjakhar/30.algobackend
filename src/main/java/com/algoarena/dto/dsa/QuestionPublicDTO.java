// src/main/java/com/algoarena/dto/dsa/QuestionPublicDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.QuestionLevel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestionPublicDTO {

    private String id;
    private String title;
    private String statement;
    private List<String> imageUrls;
    private String categoryId;
    private QuestionLevel level;
    
    // Code templates
    private Map<String, String> userStarterCode;
    // Testcases (limited to first 3)
    private List<TestcaseDTO> testcases;

    // Inner DTO for Testcase
    public static class TestcaseDTO {
        private Integer id;
        private LinkedHashMap<String, Object> input;
        private Object expectedOutput;

        public TestcaseDTO() {}

        public TestcaseDTO(QuestionDTO.TestcaseDTO testcase) {
            this.id = testcase.getId();
            this.input = testcase.getInput();
            this.expectedOutput = testcase.getExpectedOutput();
        }

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public LinkedHashMap<String, Object> getInput() { return input; }
        public void setInput(LinkedHashMap<String, Object> input) { this.input = input; }

        public Object getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(Object expectedOutput) { this.expectedOutput = expectedOutput; }
    }

    public QuestionPublicDTO() {}

    public static QuestionPublicDTO fromFull(QuestionDTO full) {
        QuestionPublicDTO dto = new QuestionPublicDTO();
        dto.id = full.getId();
        dto.title = full.getTitle();
        dto.statement = full.getStatement();
        dto.imageUrls = full.getImageUrls();
        dto.categoryId = full.getCategoryId();
        dto.level = full.getLevel();
        
        dto.userStarterCode = full.getUserStarterCode();
        
        // ✅ Only first 3 testcases
        if (full.getTestcases() != null) {
            dto.testcases = full.getTestcases().stream()
                    .limit(3)  // ✅ Limit to first 3 testcases
                    .map(TestcaseDTO::new)
                    .collect(Collectors.toList());
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

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public QuestionLevel getLevel() { return level; }
    public void setLevel(QuestionLevel level) { this.level = level; }

    public Map<String, String> getUserStarterCode() { return userStarterCode; }
    public void setUserStarterCode(Map<String, String> userStarterCode) { this.userStarterCode = userStarterCode; }

    public List<TestcaseDTO> getTestcases() { return testcases; }
    public void setTestcases(List<TestcaseDTO> testcases) { this.testcases = testcases; }
}