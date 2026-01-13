// src/main/java/com/algoarena/dto/dsa/ComplexityAnalysisDTO.java
package com.algoarena.dto.dsa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComplexityAnalysisDTO {

    @NotBlank(message = "Time complexity is required")
    @Size(max = 100, message = "Time complexity must not exceed 100 characters")
    private String timeComplexity;

    @NotBlank(message = "Space complexity is required")
    @Size(max = 100, message = "Space complexity must not exceed 100 characters")
    private String spaceComplexity;

    @Size(max = 1000, message = "Complexity description must not exceed 1000 characters")
    private String complexityDescription;

    public ComplexityAnalysisDTO() {}

    public ComplexityAnalysisDTO(String timeComplexity, String spaceComplexity) {
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
    }

    public ComplexityAnalysisDTO(String timeComplexity, String spaceComplexity, String complexityDescription) {
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