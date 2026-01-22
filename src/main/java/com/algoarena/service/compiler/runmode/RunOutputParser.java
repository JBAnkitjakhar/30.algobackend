// src/main/java/com/algoarena/service/compiler/runmode/RunOutputParser.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RunOutputParser {

    /**
     * Parse Piston stdout to extract test case results
     * Expected format:
     * TEST_CASE_START
     * EXPECTED_OUTPUT : 1
     * USER_OUTPUT : 1
     * TEST_CASE_END
     */
    public List<RunTestCaseResult> parseOutput(String stdout, int totalTestCases) {
        List<RunTestCaseResult> results = new ArrayList<>();

        if (stdout == null || stdout.isEmpty()) {
            // No output means TLE or runtime error on all tests
            for (int i = 1; i <= totalTestCases; i++) {
                RunTestCaseResult result = new RunTestCaseResult();
                result.setId(i);
                result.setStatus("TLE");
                result.setError("No output received - execution may have timed out");
                results.add(result);
            }
            return results;
        }

        String[] lines = stdout.split("\n");
        
        RunTestCaseResult currentResult = null;
        int testCaseIndex = 1;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.equals("TEST_CASE_START")) {
                currentResult = new RunTestCaseResult();
                currentResult.setId(testCaseIndex++);
                
            } else if (line.startsWith("EXPECTED_OUTPUT : ")) {
                if (currentResult != null) {
                    String expected = line.substring("EXPECTED_OUTPUT : ".length()).trim();
                    currentResult.setExpectedOutput(expected);
                }
                
            } else if (line.startsWith("USER_OUTPUT : ")) {
                if (currentResult != null) {
                    String userOutput = line.substring("USER_OUTPUT : ".length()).trim();
                    currentResult.setUserOutput(userOutput);
                }
                
            } else if (line.startsWith("ERROR : ")) {
                if (currentResult != null) {
                    String error = line.substring("ERROR : ".length()).trim();
                    currentResult.setError(error);
                    currentResult.setStatus("TLE"); // Runtime errors are TLE
                }
                
            } else if (line.equals("TEST_CASE_END")) {
                if (currentResult != null) {
                    // Determine status if not already set
                    if (currentResult.getStatus() == null) {
                        if (currentResult.getExpectedOutput() != null && 
                            currentResult.getUserOutput() != null &&
                            currentResult.getExpectedOutput().equals(currentResult.getUserOutput())) {
                            currentResult.setStatus("PASS");
                        } else {
                            currentResult.setStatus("FAIL");
                        }
                    }
                    results.add(currentResult);
                    currentResult = null;
                }
            }
        }
        
        // If some test cases didn't execute (TLE killed the process)
        while (results.size() < totalTestCases) {
            RunTestCaseResult result = new RunTestCaseResult();
            result.setId(results.size() + 1);
            result.setStatus("TLE");
            result.setError("Execution timed out before this test case could run");
            results.add(result);
        }

        return results;
    }

    /**
     * Check if stderr indicates a compile error
     */
    public boolean isCompileError(String stderr) {
        if (stderr == null || stderr.isEmpty()) {
            return false;
        }
        
        // Common compile error indicators for all languages
        return stderr.contains("error:") || 
               stderr.contains("Error:") ||
               stderr.contains("SyntaxError") ||
               stderr.contains("cannot find symbol") ||
               stderr.contains("class, interface, or enum expected") ||
               stderr.contains("compilation failed") ||
               stderr.contains("expected") ||
               stderr.contains("undefined reference") ||
               stderr.contains("NameError") ||
               stderr.contains("IndentationError") ||
               stderr.contains("ReferenceError");
    }

    /**
     * Extract compile error message (first 500 chars)
     */
    public String extractCompileError(String stderr) {
        if (stderr == null || stderr.isEmpty()) {
            return "Compilation failed";
        }
        
        // Return first 500 characters
        if (stderr.length() > 500) {
            return stderr.substring(0, 500) + "...";
        }
        
        return stderr;
    }
}