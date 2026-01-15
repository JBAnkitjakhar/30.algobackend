// src/main/java/com/algoarena/model/UserApproaches.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "user_approaches")
public class UserApproaches {

    public static final int MAX_COMBINED_SIZE_PER_QUESTION_BYTES = 20 * 1024; // 20 KB

    @Id
    private String id;

    // ✅ Removed @Version - using atomic operations now

    @Indexed
    private String userId;

    private String userName;

    private Map<String, List<ApproachData>> approaches = new HashMap<>();

    private int totalApproaches = 0;
    private LocalDateTime lastUpdated;

    public UserApproaches() {
        this.lastUpdated = LocalDateTime.now();
    }

    public UserApproaches(String userId, String userName) {
        this();
        this.id = userId;
        this.userId = userId;
        this.userName = userName;
    }

    public enum ApproachStatus {
        ACCEPTED,
        WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED
    }

    public static class ApproachData {
        private String id;
        private String questionId;

        private String textContent; // Editable
        private String codeContent; // Read-only after creation
        private String codeLanguage; // Read-only after creation

        private ApproachStatus status; // Read-only after creation
        private Long runtime; // Read-only after creation
        private Long memory; // Read-only after creation
        private ComplexityAnalysis complexityAnalysis; // Writable ONLY if null (one-time write)
        private TestcaseFailure wrongTestcase; // Read-only after creation
        private TestcaseFailure tleTestcase; // Read-only after creation

        private int contentSize;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ApproachData() {
            this.id = UUID.randomUUID().toString();
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            this.codeLanguage = "java";
            this.status = ApproachStatus.ACCEPTED; // Default
        }

        public ApproachData(String questionId, String textContent) {
            this();
            this.questionId = questionId;
            this.textContent = textContent;
            this.contentSize = calculateContentSize();
        }

        public static class ComplexityAnalysis {
            private String timeComplexity;
            private String spaceComplexity;
            private String complexityDescription;

            public ComplexityAnalysis() {
            }

            public ComplexityAnalysis(String timeComplexity, String spaceComplexity) {
                this.timeComplexity = timeComplexity;
                this.spaceComplexity = spaceComplexity;
            }

            public ComplexityAnalysis(String timeComplexity, String spaceComplexity, String complexityDescription) {
                this.timeComplexity = timeComplexity;
                this.spaceComplexity = spaceComplexity;
                this.complexityDescription = complexityDescription;
            }

            public String getTimeComplexity() {
                return timeComplexity;
            }

            public void setTimeComplexity(String timeComplexity) {
                this.timeComplexity = timeComplexity;
            }

            public String getSpaceComplexity() {
                return spaceComplexity;
            }

            public void setSpaceComplexity(String spaceComplexity) {
                this.spaceComplexity = spaceComplexity;
            }

            public String getComplexityDescription() {
                return complexityDescription;
            }

            public void setComplexityDescription(String complexityDescription) {
                this.complexityDescription = complexityDescription;
            }
        }

        public static class TestcaseFailure {
            private String input;
            private String userOutput;
            private String expectedOutput;

            public TestcaseFailure() {
            }

            public TestcaseFailure(String input, String userOutput, String expectedOutput) {
                this.input = input;
                this.userOutput = userOutput;
                this.expectedOutput = expectedOutput;
            }

            public String getInput() {
                return input;
            }

            public void setInput(String input) {
                this.input = input;
            }

            public String getUserOutput() {
                return userOutput;
            }

            public void setUserOutput(String userOutput) {
                this.userOutput = userOutput;
            }

            public String getExpectedOutput() {
                return expectedOutput;
            }

            public void setExpectedOutput(String expectedOutput) {
                this.expectedOutput = expectedOutput;
            }
        }

        public int calculateContentSize() {
            int size = 0;
            if (textContent != null) {
                size += textContent.length();
            }
            if (codeContent != null) {
                size += codeContent.length();
            }
            return size;
        }

        public void updateContentSize() {
            this.contentSize = calculateContentSize();
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getTextContent() {
            return textContent;
        }

        public void setTextContent(String textContent) {
            this.textContent = textContent;
            this.contentSize = calculateContentSize();
            this.updatedAt = LocalDateTime.now();
        }

        public String getCodeContent() {
            return codeContent;
        }

        public void setCodeContent(String codeContent) {
            this.codeContent = codeContent;
            this.contentSize = calculateContentSize();
        }

        public String getCodeLanguage() {
            return codeLanguage;
        }

        public void setCodeLanguage(String codeLanguage) {
            this.codeLanguage = codeLanguage;
        }

        public ApproachStatus getStatus() {
            return status;
        }

        public void setStatus(ApproachStatus status) {
            this.status = status;
        }

        public Long getRuntime() {
            return runtime;
        }

        public void setRuntime(Long runtime) {
            this.runtime = runtime;
        }

        public Long getMemory() {
            return memory;
        }

        public void setMemory(Long memory) {
            this.memory = memory;
        }

        public ComplexityAnalysis getComplexityAnalysis() {
            return complexityAnalysis;
        }

        public void setComplexityAnalysis(ComplexityAnalysis complexityAnalysis) {
            this.complexityAnalysis = complexityAnalysis;
        }

        public TestcaseFailure getWrongTestcase() {
            return wrongTestcase;
        }

        public void setWrongTestcase(TestcaseFailure wrongTestcase) {
            this.wrongTestcase = wrongTestcase;
        }

        public TestcaseFailure getTleTestcase() {
            return tleTestcase;
        }

        public void setTleTestcase(TestcaseFailure tleTestcase) {
            this.tleTestcase = tleTestcase;
        }

        public int getContentSize() {
            return contentSize;
        }

        public void setContentSize(int contentSize) {
            this.contentSize = contentSize;
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
            return "ApproachData{" +
                    "id='" + id + '\'' +
                    ", questionId='" + questionId + '\'' +
                    ", status=" + status +
                    ", contentSize=" + contentSize +
                    '}';
        }
    }

    // Helper methods for atomic operations
    public int getTotalSizeForQuestion(String questionId) {
        List<ApproachData> questionApproaches = approaches.get(questionId);
        if (questionApproaches == null) {
            return 0;
        }
        return questionApproaches.stream()
                .mapToInt(ApproachData::getContentSize)
                .sum();
    }

    public ApproachData findApproachById(String approachId) {
        for (List<ApproachData> questionApproaches : approaches.values()) {
            for (ApproachData approach : questionApproaches) {
                if (approach.getId().equals(approachId)) {
                    return approach;
                }
            }
        }
        return null;
    }

    public List<ApproachData> getApproachesForQuestion(String questionId) {
        return approaches.getOrDefault(questionId, new ArrayList<>());
    }

    public int getApproachCountForQuestion(String questionId) {
        return approaches.getOrDefault(questionId, new ArrayList<>()).size();
    }

    public List<ApproachData> getAllApproachesFlat() {
        List<ApproachData> allApproaches = new ArrayList<>();
        for (List<ApproachData> questionApproaches : approaches.values()) {
            allApproaches.addAll(questionApproaches);
        }
        allApproaches.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return allApproaches;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.userId = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.id = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Map<String, List<ApproachData>> getApproaches() {
        return approaches;
    }

    public void setApproaches(Map<String, List<ApproachData>> approaches) {
        this.approaches = approaches;
    }

    public int getTotalApproaches() {
        return totalApproaches;
    }

    public void setTotalApproaches(int totalApproaches) {
        this.totalApproaches = totalApproaches;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "UserApproaches{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", totalApproaches=" + totalApproaches +
                ", questionsWithApproaches=" + approaches.size() +
                '}';
    }
}