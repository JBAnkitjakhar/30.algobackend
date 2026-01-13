// src/main/java/com/algoarena/model/SubmissionTracking.java
package com.algoarena.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "submission_tracking")
public class SubmissionTracking {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private List<DailySubmission> submissionHistory = new ArrayList<>();

    public static class DailySubmission {
        private LocalDate date;
        private Integer count;

        public DailySubmission() {
        }

        public DailySubmission(LocalDate date, Integer count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<DailySubmission> getSubmissionHistory() {
        return submissionHistory;
    }

    public void setSubmissionHistory(List<DailySubmission> submissionHistory) {
        this.submissionHistory = submissionHistory;
    }
}