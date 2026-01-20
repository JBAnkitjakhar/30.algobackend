// src/main/java/com/algoarena/service/compiler/CodeMergerService.java
package com.algoarena.service.compiler;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodeMergerService {

    public String mergeCode(String userCode, String template, String language) {
        if ("java".equalsIgnoreCase(language)) {
            return mergeJavaCode(userCode, template);
        } else if ("cpp".equalsIgnoreCase(language) || "c++".equalsIgnoreCase(language)) {
            return mergeCppCode(userCode, template);
        }
        
        return template.replace("// Your implementation here", userCode);
    }

    /**
     * For Java: Replace the placeholder Solution class with user's Solution class
     * and ensure Main class is public
     */
    private String mergeJavaCode(String userCode, String template) {
        // Find where Solution class starts in template
        int solutionStart = template.indexOf("class Solution {");
        if (solutionStart == -1) {
            return template; // Fallback
        }
        
        // Find where Solution class ends (find matching closing brace)
        int braceCount = 0;
        int solutionEnd = -1;
        boolean foundFirstBrace = false;
        
        for (int i = solutionStart; i < template.length(); i++) {
            if (template.charAt(i) == '{') {
                braceCount++;
                foundFirstBrace = true;
            } else if (template.charAt(i) == '}') {
                braceCount--;
                if (foundFirstBrace && braceCount == 0) {
                    solutionEnd = i + 1; // Include the closing brace
                    break;
                }
            }
        }
        
        if (solutionEnd == -1) {
            return template;
        }
        
        // Replace template's Solution class with user's Solution class
        String before = template.substring(0, solutionStart);
        String after = template.substring(solutionEnd);
        
        // CRITICAL FIX: Remove "public" from Main class declaration
        // Piston needs the file to match the public class name
        String mergedCode = before + userCode.trim() + "\n\n" + after;
        
        // Make Solution class package-private (not public) if it exists in user code
        mergedCode = mergedCode.replace("public class Solution", "class Solution");
        
        return mergedCode;
    }

    /**
     * For C++: Replace the placeholder Solution class with user's Solution class
     */
    private String mergeCppCode(String userCode, String template) {
        // Find where Solution class starts in template
        int solutionStart = template.indexOf("class Solution {");
        if (solutionStart == -1) {
            return template; // Fallback
        }
        
        // Find where Solution class ends
        int braceCount = 0;
        int solutionEnd = -1;
        boolean foundFirstBrace = false;
        
        for (int i = solutionStart; i < template.length(); i++) {
            if (template.charAt(i) == '{') {
                braceCount++;
                foundFirstBrace = true;
            } else if (template.charAt(i) == '}') {
                braceCount--;
                if (foundFirstBrace && braceCount == 0) {
                    solutionEnd = i + 2; // Include closing brace + semicolon
                    break;
                }
            }
        }
        
        if (solutionEnd == -1) {
            return template;
        }
        
        // Replace template's Solution class with user's Solution class
        String before = template.substring(0, solutionStart);
        String after = template.substring(solutionEnd);
        
        return before + userCode.trim() + "\n\n" + after;
    }

    public String formatInput(Map<String, Object> input, String language) {
        if ("java".equalsIgnoreCase(language)) {
            return formatJavaInput(input);
        } else if ("cpp".equalsIgnoreCase(language) || "c++".equalsIgnoreCase(language)) {
            return formatCppInput(input);
        }
        
        return input.toString();
    }

    private String formatJavaInput(Map<String, Object> input) {
        Object numsObj = input.get("nums");
        
        if (numsObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> numsList = (java.util.List<Integer>) numsObj;
            
            StringBuilder sb = new StringBuilder();
            sb.append(numsList.size()).append("\n");
            
            for (int i = 0; i < numsList.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(numsList.get(i));
            }
            
            return sb.toString();
        }
        
        return input.toString();
    }

    private String formatCppInput(Map<String, Object> input) {
        Object numsObj = input.get("nums");
        
        if (numsObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> numsList = (java.util.List<Integer>) numsObj;
            
            StringBuilder sb = new StringBuilder();
            sb.append(numsList.size()).append("\n");
            
            for (int i = 0; i < numsList.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(numsList.get(i));
            }
            
            return sb.toString();
        }
        
        return input.toString();
    }
}