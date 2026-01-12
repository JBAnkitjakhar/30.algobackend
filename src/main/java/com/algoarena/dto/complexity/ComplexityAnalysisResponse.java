//src/main/java/com/algoarena/dto/complexity/ComplexityAnalysisResponse.java

package com.algoarena.dto.complexity;

public class ComplexityAnalysisResponse {
    
    private String timeComplexity;
    private String spaceComplexity;
    private String complexityDescription;
    
    // Constructors
    public ComplexityAnalysisResponse() {
    }
    
    public ComplexityAnalysisResponse(String timeComplexity, String spaceComplexity, String complexityDescription) {
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.complexityDescription = complexityDescription;
    }
    
    // Getters and Setters
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