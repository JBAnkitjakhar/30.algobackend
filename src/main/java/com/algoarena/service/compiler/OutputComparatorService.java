// src/main/java/com/algoarena/service/compiler/OutputComparatorService.java
package com.algoarena.service.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OutputComparatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Compare user output with expected output
     */
    public boolean compare(String userOutput, Object expectedOutput) {
        try {
            // Normalize both
            String normalizedUser = normalize(userOutput);
            String normalizedExpected = normalize(expectedOutput.toString());

            return normalizedUser.equals(normalizedExpected);
        } catch (Exception e) {
            return false;
        }
    }

    private String normalize(String output) {
        // Remove all whitespace
        String cleaned = output.replaceAll("\\s+", "");
        
        try {
            // Try parsing as JSON and re-serialize for consistent formatting
            Object parsed = objectMapper.readValue(cleaned, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            return cleaned;
        }
    }
}