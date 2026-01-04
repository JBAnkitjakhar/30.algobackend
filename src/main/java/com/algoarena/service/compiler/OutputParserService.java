// src/main/java/com/algoarena/service/compiler/OutputParserService.java
package com.algoarena.service.compiler;

import com.algoarena.dto.compiler.TestCaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OutputParserService {

    /**
     * Parse Piston stdout to extract individual testcase results
     * Expected format:
     * TC_START:0
     * OUTPUT:[[1,2],[3,4]]
     * TIME:45
     * TC_END:0
     */
    public List<TestCaseResult> parseTestCaseResults(String stdout) {
        List<TestCaseResult> results = new ArrayList<>();

        if (stdout == null || stdout.isEmpty()) {
            return results;
        }

        // Split by testcase blocks
        String[] lines = stdout.split("\n");
        
        TestCaseResult currentResult = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("TC_START:")) {
                // Start new testcase
                int index = extractIndex(line, "TC_START:");
                currentResult = new TestCaseResult();
                currentResult.setIndex(index);
                currentResult.setStatus("success");
                
            } else if (line.startsWith("OUTPUT:")) {
                // Extract output
                if (currentResult != null) {
                    String output = line.substring("OUTPUT:".length()).trim();
                    currentResult.setOutput(output);
                }
                
            } else if (line.startsWith("TIME:")) {
                // Extract time
                if (currentResult != null) {
                    Long timeMs = extractTime(line, "TIME:");
                    currentResult.setTimeMs(timeMs);
                }
                
            } else if (line.startsWith("TC_END:")) {
                // End testcase and add to results
                if (currentResult != null) {
                    results.add(currentResult);
                    currentResult = null;
                }
            }
        }
        
        // If parsing was incomplete (TLE/error), add partial result
        if (currentResult != null) {
            currentResult.setStatus("incomplete");
            results.add(currentResult);
        }

        return results;
    }

    /**
     * Extract index from line like "TC_START:5"
     */
    private int extractIndex(String line, String prefix) {
        try {
            String indexStr = line.substring(prefix.length()).trim();
            return Integer.parseInt(indexStr);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Extract time from line like "TIME:45"
     */
    private Long extractTime(String line, String prefix) {
        try {
            String timeStr = line.substring(prefix.length()).trim();
            return Long.parseLong(timeStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if stdout indicates a compile error
     */
    public boolean hasCompileError(String stderr) {
        if (stderr == null || stderr.isEmpty()) {
            return false;
        }
        
        // Common compile error indicators
        return stderr.contains("error:") || 
               stderr.contains("Error:") ||
               stderr.contains("SyntaxError") ||
               stderr.contains("compilation failed");
    }

    /**
     * Extract compile error message
     */
    public String extractCompileError(String stderr) {
        if (stderr == null) {
            return "Compilation failed";
        }
        
        // Return first 500 characters of error
        if (stderr.length() > 500) {
            return stderr.substring(0, 500) + "...";
        }
        
        return stderr;
    }
}