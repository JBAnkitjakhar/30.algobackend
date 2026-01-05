// src/main/java/com/algoarena/dto/dsa/ApproachUpdateDTO.java
package com.algoarena.dto.dsa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating approach - ONLY textContent is editable
 */
public class ApproachUpdateDTO {

    @NotBlank(message = "Text content is required")
    @Size(min = 10, max = 15000, message = "Text content must be between 10 and 15000 characters")
    private String textContent;

    public ApproachUpdateDTO() {}

    public ApproachUpdateDTO(String textContent) {
        this.textContent = textContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
}