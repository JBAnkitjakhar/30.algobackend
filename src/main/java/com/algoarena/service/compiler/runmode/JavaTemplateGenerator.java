// src/main/java/com/algoarena/service/compiler/runmode/JavaTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaTemplateGenerator {

    /**
     * Generates complete Java code for execution
     * 
     * @param correctSolution The correct solution class code from DB
     * @param userCode The user's solution code
     * @param testCases List of test cases from frontend
     * @param methodName The method name to call (from DB)
     * @return Complete Java code ready for Piston execution
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
        template.append("import java.util.*;\n\n");
        
        // 2. Main class with test cases
        template.append(generateMainClass(signature, testCases));
        
        // 3. Correct solution (rename Solution -> CorrectSolution)
        template.append("\n// ===== CORRECT SOLUTION =====\n");
        template.append(correctSolution.replace("class Solution", "class CorrectSolution"));
        template.append("\n\n");
        
        // 4. User solution
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n");
        
        return template.toString();
    }

    /**
     * ✅ UPDATED: Extract method signature for the SPECIFIC methodName
     * Example: Find "public int numDistinctIslands(int[][] grid)"
     */
    private MethodSignature extractMethodSignature(String correctSolution, String methodName) {
        // Pattern to match: public returnType METHODNAME(params)
        // Using \\b for word boundary to match exact method name
        String patternString = "public\\s+(\\w+(?:\\[\\])*?)\\s+" + 
                               Pattern.quote(methodName) + 
                               "\\s*\\(([^)]*)\\)";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(correctSolution);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Could not find method '" + methodName + "' in correct solution");
        }
        
        String returnType = matcher.group(1).trim();
        String paramsString = matcher.group(2).trim();
        
        List<Parameter> parameters = new ArrayList<>();
        
        if (!paramsString.isEmpty()) {
            String[] paramPairs = paramsString.split(",");
            for (String pair : paramPairs) {
                String[] parts = pair.trim().split("\\s+");
                if (parts.length >= 2) {
                    String type = parts[0];
                    String name = parts[1];
                    parameters.add(new Parameter(type, name));
                }
            }
        }
        
        return new MethodSignature(returnType, methodName, parameters);
    }

    /**
     * Generate Main class with all test cases
     */
    private String generateMainClass(MethodSignature signature, List<RunTestCaseInput> testCases) {
        StringBuilder main = new StringBuilder();
        
        main.append("public class Main {\n");
        main.append("    public static void main(String[] args) {\n");
        main.append("        CorrectSolution correctSolution = new CorrectSolution();\n");
        main.append("        Solution userSolution = new Solution();\n\n");
        
        // Generate each test case using array index
        for (int i = 0; i < testCases.size(); i++) {
            main.append(generateTestCase(testCases.get(i), i + 1, signature));
        }
        
        main.append("    }\n\n");
        
        // Add helper method
        main.append(generateRunTestCaseMethod(signature));
        
        // Add deep copy methods if needed
        main.append(generateDeepCopyMethods(signature));
        
        main.append("}\n");
        
        return main.toString();
    }

    /**
     * Generate single test case code
     * @param testCase The test case data
     * @param testNumber Test case number (index + 1)
     * @param signature Method signature
     */
    private String generateTestCase(RunTestCaseInput testCase, int testNumber, MethodSignature signature) {
        StringBuilder code = new StringBuilder();
        
        code.append("        // ===== TEST CASE ").append(testNumber).append(" =====\n");
        
        Map<String, Object> inputs = testCase.getInput();
        List<String> varNames = new ArrayList<>();
        
        // Generate variable for each parameter
        for (Parameter param : signature.getParameters()) {
            String varName = param.getName() + testNumber;
            varNames.add(varName);
            
            Object value = inputs.get(param.getName());
            String javaCode = convertToJavaCode(value, param.getType());
            
            code.append("        ")
                .append(param.getType())
                .append(" ")
                .append(varName)
                .append(" = ")
                .append(javaCode)
                .append(";\n");
        }
        
        // Generate method call
        code.append("        runTestCase(");
        
        for (int i = 0; i < varNames.size(); i++) {
            code.append(varNames.get(i));
            if (i < varNames.size() - 1) {
                code.append(", ");
            }
        }
        
        code.append(", correctSolution, userSolution);\n\n");
        
        return code.toString();
    }

    /**
     * Convert JSON value to Java code string
     */
    private String convertToJavaCode(Object value, String type) {
        if (value == null) {
            return "null";
        }
        
        // Handle 2D arrays: int[][]
        if (type.equals("int[][]")) {
            return convert2DIntArray((List<?>) value);
        }
        
        // Handle 1D arrays: int[]
        if (type.equals("int[]")) {
            return convert1DIntArray((List<?>) value);
        }
        
        // Handle String arrays: String[]
        if (type.equals("String[]")) {
            return convertStringArray((List<?>) value);
        }
        
        // Handle primitives
        if (type.equals("int") || type.equals("long") || type.equals("double") || type.equals("float")) {
            return String.valueOf(value);
        }
        
        if (type.equals("boolean")) {
            return String.valueOf(value);
        }
        
        if (type.equals("char")) {
            return "'" + value + "'";
        }
        
        if (type.equals("String")) {
            return "\"" + escapeString(String.valueOf(value)) + "\"";
        }
        
        // Default: try to convert
        return String.valueOf(value);
    }

    private String convert2DIntArray(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "new int[0][0]";
        }
        
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            List<?> row = (List<?>) array.get(i);
            sb.append("{");
            for (int j = 0; j < row.size(); j++) {
                sb.append(row.get(j));
                if (j < row.size() - 1) sb.append(",");
            }
            sb.append("}");
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DIntArray(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "new int[0]";
        }
        
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convertStringArray(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "new String[0]";
        }
        
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append("\"").append(escapeString(String.valueOf(array.get(i)))).append("\"");
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\t", "\\t");
    }

    /**
     * Generate runTestCase method based on signature
     */
    private String generateRunTestCaseMethod(MethodSignature signature) {
        StringBuilder method = new StringBuilder();
        
        method.append("    private static void runTestCase(");
        
        // Add parameters
        for (int i = 0; i < signature.getParameters().size(); i++) {
            Parameter param = signature.getParameters().get(i);
            method.append(param.getType()).append(" ").append(param.getName());
            if (i < signature.getParameters().size() - 1) {
                method.append(", ");
            }
        }
        
        method.append(", CorrectSolution correct, Solution user) {\n");
        method.append("        try {\n");
        
        // Deep copy parameters for both solutions
        List<String> correctParams = new ArrayList<>();
        List<String> userParams = new ArrayList<>();
        
        for (Parameter param : signature.getParameters()) {
            String correctVar = param.getName() + "Correct";
            String userVar = param.getName() + "User";
            
            correctParams.add(correctVar);
            userParams.add(userVar);
            
            if (needsDeepCopy(param.getType())) {
                method.append("            ")
                      .append(param.getType())
                      .append(" ")
                      .append(correctVar)
                      .append(" = deepCopy(")
                      .append(param.getName())
                      .append(");\n");
                
                method.append("            ")
                      .append(param.getType())
                      .append(" ")
                      .append(userVar)
                      .append(" = deepCopy(")
                      .append(param.getName())
                      .append(");\n");
            } else {
                method.append("            ")
                      .append(param.getType())
                      .append(" ")
                      .append(correctVar)
                      .append(" = ")
                      .append(param.getName())
                      .append(";\n");
                
                method.append("            ")
                      .append(param.getType())
                      .append(" ")
                      .append(userVar)
                      .append(" = ")
                      .append(param.getName())
                      .append(";\n");
            }
        }
        
        method.append("\n");
        
        // Call both solutions
        method.append("            ")
              .append(signature.getReturnType())
              .append(" expected = correct.")
              .append(signature.getMethodName())
              .append("(")
              .append(String.join(", ", correctParams))
              .append(");\n");
        
        method.append("            ")
              .append(signature.getReturnType())
              .append(" actual = user.")
              .append(signature.getMethodName())
              .append("(")
              .append(String.join(", ", userParams))
              .append(");\n\n");
        
        // Print results
        method.append("            System.out.println(\"TEST_CASE_START\");\n");
        method.append("            System.out.println(\"EXPECTED_OUTPUT : \" + expected);\n");
        method.append("            System.out.println(\"USER_OUTPUT : \" + actual);\n");
        method.append("            System.out.println(\"TEST_CASE_END\");\n\n");
        
        // Exception handling
        method.append("        } catch (Exception e) {\n");
        method.append("            System.out.println(\"TEST_CASE_START\");\n");
        method.append("            System.out.println(\"ERROR : \" + e.getMessage());\n");
        method.append("            System.out.println(\"TEST_CASE_END\");\n");
        method.append("        }\n");
        method.append("    }\n\n");
        
        return method.toString();
    }

    private boolean needsDeepCopy(String type) {
        return type.contains("[]") || type.equals("List") || type.equals("Map");
    }

    /**
     * Generate deep copy utility methods
     */
    private String generateDeepCopyMethods(MethodSignature signature) {
        StringBuilder methods = new StringBuilder();
        
        // Check if we need int[][] deep copy
        boolean needsIntArray2D = signature.getParameters().stream()
            .anyMatch(p -> p.getType().equals("int[][]"));
        
        boolean needsIntArray1D = signature.getParameters().stream()
            .anyMatch(p -> p.getType().equals("int[]"));
        
        if (needsIntArray2D) {
            methods.append("    private static int[][] deepCopy(int[][] original) {\n");
            methods.append("        if (original == null) return null;\n");
            methods.append("        int[][] copy = new int[original.length][];\n");
            methods.append("        for (int i = 0; i < original.length; i++) {\n");
            methods.append("            copy[i] = Arrays.copyOf(original[i], original[i].length);\n");
            methods.append("        }\n");
            methods.append("        return copy;\n");
            methods.append("    }\n\n");
        }
        
        if (needsIntArray1D) {
            methods.append("    private static int[] deepCopy(int[] original) {\n");
            methods.append("        if (original == null) return null;\n");
            methods.append("        return Arrays.copyOf(original, original.length);\n");
            methods.append("    }\n\n");
        }
        
        return methods.toString();
    }

    // Inner classes for type safety
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