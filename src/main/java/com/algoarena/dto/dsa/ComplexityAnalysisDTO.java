// src/main/java/com/algoarena/dto/dsa/ComplexityAnalysisDTO.java
package com.algoarena.dto.dsa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for adding complexity analysis (one-time only)
 */
public class ComplexityAnalysisDTO {

    @NotBlank(message = "Time complexity is required")
    @Size(max = 100, message = "Time complexity must not exceed 100 characters")
    private String timeComplexity;

    @NotBlank(message = "Space complexity is required")
    @Size(max = 100, message = "Space complexity must not exceed 100 characters")
    private String spaceComplexity;

    public ComplexityAnalysisDTO() {}

    public ComplexityAnalysisDTO(String timeComplexity, String spaceComplexity) {
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
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
}