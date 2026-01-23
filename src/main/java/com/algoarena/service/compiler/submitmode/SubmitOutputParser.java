// src/main/java/com/algoarena/service/compiler/submitmode/SubmitOutputParser.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.dto.compiler.submitmode.SubmitTestCaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubmitOutputParser {

    /**
     * ✅ UPDATED: Now also parses EXECUTION_TIME per testcase
     */
    public List<SubmitTestCaseResult> parseOutput(String stdout, int totalTestCases) {
        List<SubmitTestCaseResult> results = new ArrayList<>();

        if (stdout == null || stdout.isEmpty()) {
            for (int i = 1; i <= totalTestCases; i++) {
                SubmitTestCaseResult result = new SubmitTestCaseResult();
                result.setId(i);
                result.setStatus("TLE");
                result.setError("No output received - execution may have timed out");
                result.setExecutionTime(0L); // ✅ NEW
                results.add(result);
            }
            return results;
        }

        String[] lines = stdout.split("\n");
        
        SubmitTestCaseResult currentResult = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.equals("TEST_CASE_START")) {
                currentResult = new SubmitTestCaseResult();
                
            } else if (line.startsWith("TEST_CASE_ID : ")) {
                if (currentResult != null) {
                    int id = Integer.parseInt(line.substring("TEST_CASE_ID : ".length()).trim());
                    currentResult.setId(id);
                }
                
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
                
            } else if (line.startsWith("EXECUTION_TIME : ")) { // ✅ NEW
                if (currentResult != null) {
                    try {
                        long time = Long.parseLong(line.substring("EXECUTION_TIME : ".length()).trim());
                        currentResult.setExecutionTime(time);
                    } catch (NumberFormatException e) {
                        currentResult.setExecutionTime(0L);
                    }
                }
                
            } else if (line.startsWith("ERROR : ")) {
                if (currentResult != null) {
                    String error = line.substring("ERROR : ".length()).trim();
                    currentResult.setError(error);
                    currentResult.setStatus("FAIL");
                    currentResult.setExecutionTime(0L); // ✅ NEW
                }
                
            } else if (line.equals("TEST_CASE_END")) {
                if (currentResult != null) {
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
        
        // Fill missing test cases (TLE)
        while (results.size() < totalTestCases) {
            SubmitTestCaseResult result = new SubmitTestCaseResult();
            result.setId(results.size() + 1);
            result.setStatus("TLE");
            result.setError("Execution timed out before this test case could run");
            result.setExecutionTime(0L); // ✅ NEW
            results.add(result);
        }

        return results;
    }

    public boolean isCompileError(String stderr) {
        if (stderr == null || stderr.isEmpty()) return false;
        
        return stderr.contains("error:") || 
               stderr.contains("Error:") ||
               stderr.contains("SyntaxError") ||
               stderr.contains("cannot find symbol") ||
               stderr.contains("class, interface, or enum expected") ||
               stderr.contains("compilation failed");
    }

    public String extractCompileError(String stderr) {
        if (stderr == null || stderr.isEmpty()) return "Compilation failed";
        return stderr.length() > 500 ? stderr.substring(0, 500) + "..." : stderr;
    }
}