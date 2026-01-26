// src/main/java/com/algoarena/model/Question.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    @Version
    private Long version;

    private String title;
    private String statement;

    private List<String> imageUrls;

    @Indexed
    private String categoryId;

    private QuestionLevel level;
    private Integer displayOrder;

    // Code templates (multi-language)
    private Map<String, String> userStarterCode;
    private Map<String, String> submitTemplate;   
    private Map<String, String> runTemplate;          

    private String methodName;

    // Testcases
    private List<Testcase> testcases;

    // Metadata
    private String createdById;
    private String createdByName;

    @Indexed(name = "createdAt_idx")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Inner class for Testcase
    public static class Testcase {
        private Integer id;
        private LinkedHashMap<String, Object> input;
        private Object expectedOutput;

        public Testcase() {
        }

        public Testcase(Integer id, LinkedHashMap<String, Object> input, Object expectedOutput) {
            this.id = id;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public LinkedHashMap<String, Object> getInput() {
            return input;
        }

        public void setInput(LinkedHashMap<String, Object> input) {
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
    public Question() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getUserStarterCode() {
        return userStarterCode;
    }

    public void setUserStarterCode(Map<String, String> userStarterCode) {
        this.userStarterCode = userStarterCode;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getSubmitTemplate() {  // ✅ RENAMED
        return submitTemplate;
    }

    public void setSubmitTemplate(Map<String, String> submitTemplate) {  // ✅ RENAMED
        this.submitTemplate = submitTemplate;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getRunTemplate() {  // ✅ RENAMED
        return runTemplate;
    }

    public void setRunTemplate(Map<String, String> runTemplate) {  // ✅ RENAMED
        this.runTemplate = runTemplate;
        this.updatedAt = LocalDateTime.now();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
        this.updatedAt = LocalDateTime.now();
    }

    public List<Testcase> getTestcases() {
        return testcases;
    }

    public void setTestcases(List<Testcase> testcases) {
        this.testcases = testcases;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public QuestionLevel getLevel() {
        return level;
    }

    public void setLevel(QuestionLevel level) {
        this.level = level;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", level=" + level +
                ", methodName='" + methodName + '\'' +
                ", version=" + version +
                '}';
    }
}