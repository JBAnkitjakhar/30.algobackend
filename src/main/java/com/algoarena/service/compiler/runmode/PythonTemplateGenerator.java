// src/main/java/com/algoarena/service/compiler/runmode/PythonTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PythonTemplateGenerator {

    /**
     * Generates complete Python code for execution
     */
    public String generateRunTemplate(
            String correctSolution,
            String userCode,
            List<RunTestCaseInput> testCases,
            String methodName) { // ✅ NEW parameter
        
        // ✅ Extract method signature using the provided methodName
        MethodSignature signature = extractMethodSignature(correctSolution, methodName);
        
        StringBuilder template = new StringBuilder();
        
        // 1. Imports
        template.append("from typing import List, Optional\n");
        template.append("import sys\n");
        template.append("import copy\n\n");
        
        // 2. Correct solution (rename Solution -> CorrectSolution)
        template.append("# ===== CORRECT SOLUTION =====\n");
        template.append(correctSolution.replace("class Solution:", "class CorrectSolution:"));
        template.append("\n\n");
        
        // 3. User solution
        template.append("# ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");
        
        // 4. Main execution with test cases
        template.append(generateMainExecution(signature, testCases));
        
        return template.toString();
    }

    /**
     * ✅ UPDATED: Extract method signature for the SPECIFIC methodName
     */
    private MethodSignature extractMethodSignature(String correctSolution, String methodName) {
        // Pattern: def METHODNAME(self, params) -> returnType:
        String patternString = "def\\s+" + 
                               Pattern.quote(methodName) + 
                               "\\s*\\(self(?:,\\s*([^)]*))?\\)(?:\\s*->\\s*(\\w+(?:\\[.*?\\])?))?\\s*:";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(correctSolution);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Could not find method '" + methodName + "' in correct solution");
        }
        
        String paramsString = matcher.group(1);
        String returnType = matcher.group(2) != null ? matcher.group(2) : "None";
        
        List<Parameter> parameters = new ArrayList<>();
        
        if (paramsString != null && !paramsString.trim().isEmpty()) {
            String[] paramPairs = paramsString.split(",");
            for (String pair : paramPairs) {
                // Extract name and type from "grid: List[List[int]]"
                String[] parts = pair.trim().split(":");
                if (parts.length >= 1) {
                    String name = parts[0].trim();
                    String type = parts.length > 1 ? parts[1].trim() : "Any";
                    parameters.add(new Parameter(type, name));
                }
            }
        }
        
        return new MethodSignature(returnType, methodName, parameters);
    }

    /**
     * Generate main execution with all test cases
     */
    private String generateMainExecution(MethodSignature signature, List<RunTestCaseInput> testCases) {
        StringBuilder main = new StringBuilder();
        
        main.append("if __name__ == \"__main__\":\n");
        main.append("    correct_solution = CorrectSolution()\n");
        main.append("    user_solution = Solution()\n\n");
        
        // Generate each test case
        for (int i = 0; i < testCases.size(); i++) {
            main.append(generateTestCase(testCases.get(i), i + 1, signature));
        }
        
        return main.toString();
    }

    /**
     * Generate single test case code
     */
    private String generateTestCase(RunTestCaseInput testCase, int testNumber, MethodSignature signature) {
        StringBuilder code = new StringBuilder();
        
        code.append("    # ===== TEST CASE ").append(testNumber).append(" =====\n");
        
        Map<String, Object> inputs = testCase.getInput();
        
        // Generate variables
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String pythonCode = convertToPythonCode(value);
            code.append("    ").append(param.getName()).append(" = ").append(pythonCode).append("\n");
        }
        
        code.append("\n");
        code.append("    try:\n");
        
        // Create deep copies for correct solution
        code.append("        # Deep copy for correct solution\n");
        for (Parameter param : signature.getParameters()) {
            if (param.getType().contains("List")) {
                code.append("        ").append(param.getName()).append("_correct = copy.deepcopy(")
                    .append(param.getName()).append(")\n");
            }
        }
        
        code.append("\n");
        
        // Call correct solution with deep copied parameters
        code.append("        expected = correct_solution.").append(signature.getMethodName()).append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName();
            String paramType = signature.getParameters().get(i).getType();
            
            if (paramType.contains("List")) {
                code.append(paramName).append("_correct");
            } else {
                code.append(paramName);
            }
            
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(")\n");
        
        // Create deep copies for user solution
        code.append("\n        # Deep copy for user solution\n");
        for (Parameter param : signature.getParameters()) {
            if (param.getType().contains("List")) {
                code.append("        ").append(param.getName()).append("_user = copy.deepcopy(")
                    .append(param.getName()).append(")\n");
            }
        }
        
        code.append("\n");
        
        // Call user solution with deep copied parameters
        code.append("        actual = user_solution.").append(signature.getMethodName()).append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName();
            String paramType = signature.getParameters().get(i).getType();
            
            if (paramType.contains("List")) {
                code.append(paramName).append("_user");
            } else {
                code.append(paramName);
            }
            
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(")\n\n");
        
        // Print results - handle None returns
        code.append("        print(\"TEST_CASE_START\")\n");
        code.append("        print(f\"EXPECTED_OUTPUT : {expected if expected is not None else 'None'}\")\n");
        code.append("        print(f\"USER_OUTPUT : {actual if actual is not None else 'None'}\")\n");
        code.append("        print(\"TEST_CASE_END\")\n");
        
        code.append("    except Exception as e:\n");
        code.append("        print(\"TEST_CASE_START\")\n");
        code.append("        print(f\"ERROR : {str(e)}\")\n");
        code.append("        print(\"TEST_CASE_END\")\n\n");
        
        return code.toString();
    }

    /**
     * Convert JSON value to Python code string
     */
    private String convertToPythonCode(Object value) {
        if (value == null) {
            return "None";
        }
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            
            // Check if it's a 2D array
            if (list.get(0) instanceof List) {
                return convert2DList(list);
            } else {
                return convert1DList(list);
            }
        }
        
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "True" : "False";
        }
        
        return String.valueOf(value);
    }

    private String convert2DList(List<?> array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            List<?> row = (List<?>) array.get(i);
            sb.append("[");
            for (int j = 0; j < row.size(); j++) {
                sb.append(row.get(j));
                if (j < row.size() - 1) sb.append(",");
            }
            sb.append("]");
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String convert1DList(List<?> array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            if (item instanceof String) {
                sb.append("\"").append(escapeString((String) item)).append("\"");
            } else {
                sb.append(item);
            }
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\t", "\\t");
    }

    // Inner classes
    public static class MethodSignature {
        private final String returnType;
        private final String methodName;
        private final List<Parameter> parameters;

        public MethodSignature(String returnType, String methodName, List<Parameter> parameters) {
            this.returnType = returnType;
            this.methodName = methodName;
            this.parameters = parameters;
        }

        public String getReturnType() { return returnType; }
        public String getMethodName() { return methodName; }
        public List<Parameter> getParameters() { return parameters; }
    }

    public static class Parameter {
        private final String type;
        private final String name;

        public Parameter(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() { return type; }
        public String getName() { return name; }
    }
}