// src/main/java/com/algoarena/dto/dsa/SubmissionHistoryDTO.java
package com.algoarena.dto.dsa;

import com.algoarena.model.SubmissionTracking;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SubmissionHistoryDTO {

    private String userId;
    private List<DailySubmissionDTO> submissionHistory;
    private int totalSubmissions;
    private int totalDays;

    public static class DailySubmissionDTO {
        private LocalDate date;
        private Integer count;

        public DailySubmissionDTO(LocalDate date, Integer count) {
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

    public SubmissionHistoryDTO(SubmissionTracking tracking) {
        this.userId = tracking.getUserId();
        this.submissionHistory = tracking.getSubmissionHistory().stream()
                .map(ds -> new DailySubmissionDTO(ds.getDate(), ds.getCount()))
                .collect(Collectors.toList());
        this.totalDays = this.submissionHistory.size();
        this.totalSubmissions = this.submissionHistory.stream()
                .mapToInt(DailySubmissionDTO::getCount)
                .sum();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<DailySubmissionDTO> getSubmissionHistory() {
        return submissionHistory;
    }

    public void setSubmissionHistory(List<DailySubmissionDTO> submissionHistory) {
        this.submissionHistory = submissionHistory;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(int totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }
}