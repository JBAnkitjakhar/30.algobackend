// src/main/java/com/algoarena/service/compiler/runmode/JavaTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaTemplateGenerator {

    public String generateRunTemplate(
            String correctSolution,
            String userCode,
            List<RunTestCaseInput> testCases,
            String methodName) {
        
        MethodSignature signature = extractMethodSignature(correctSolution, methodName);
        
        StringBuilder template = new StringBuilder();
        
        template.append("import java.util.*;\n\n");
        template.append(generateMainClass(signature, testCases));
        
        template.append("\n// ===== CORRECT SOLUTION =====\n");
        template.append(correctSolution.replace("class Solution", "class CorrectSolution"));
        template.append("\n\n");
        
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n");
        
        return template.toString();
    }

    private MethodSignature extractMethodSignature(String correctSolution, String methodName) {
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

    private String generateMainClass(MethodSignature signature, List<RunTestCaseInput> testCases) {
        StringBuilder main = new StringBuilder();
        
        main.append("public class Main {\n");
        main.append("    public static void main(String[] args) {\n\n");
        
        // Generate each test case
        for (int i = 0; i < testCases.size(); i++) {
            main.append(generateTestCase(testCases.get(i), i + 1, signature));
        }
        
        main.append("    }\n");
        main.append("}\n");
        
        return main.toString();
    }

    // ✅ UPDATED: Fresh instances + deep copy per test case
    private String generateTestCase(RunTestCaseInput testCase, int testNumber, MethodSignature signature) {
        StringBuilder code = new StringBuilder();
        
        code.append("        // ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("        try {\n");
        
        // ✅ CREATE FRESH INSTANCES
        code.append("            CorrectSolution correctSolution").append(testNumber).append(" = new CorrectSolution();\n");
        code.append("            Solution userSolution").append(testNumber).append(" = new Solution();\n\n");
        
        Map<String, Object> inputs = testCase.getInput();
        
        // Generate variables for CORRECT solution
        code.append("            // Variables for correct solution\n");
        for (Parameter param : signature.getParameters()) {
            String varName = param.getName() + testNumber + "Correct";
            Object value = inputs.get(param.getName());
            String javaCode = convertToJavaCode(value, param.getType());
            
            code.append("            ")
                .append(param.getType())
                .append(" ")
                .append(varName)
                .append(" = ")
                .append(javaCode)
                .append(";\n");
        }
        
        // ✅ DEEP COPY for correct solution if needed
        for (Parameter param : signature.getParameters()) {
            if (param.getType().equals("int[][]")) {
                String varName = param.getName() + testNumber + "Correct";
                code.append("            // Deep copy for correct solution\n");
                code.append("            int[][] ").append(varName).append("Copy = new int[")
                    .append(varName).append(".length][];\n");
                code.append("            for (int i = 0; i < ").append(varName).append(".length; i++) {\n");
                code.append("                ").append(varName).append("Copy[i] = java.util.Arrays.copyOf(")
                    .append(varName).append("[i], ").append(varName).append("[i].length);\n");
                code.append("            }\n");
            } else if (param.getType().equals("int[]")) {
                String varName = param.getName() + testNumber + "Correct";
                code.append("            // Deep copy for correct solution\n");
                code.append("            int[] ").append(varName).append("Copy = java.util.Arrays.copyOf(")
                    .append(varName).append(", ").append(varName).append(".length);\n");
            }
        }
        
        code.append("\n");
        
        // Generate variables for USER solution
        code.append("            // Variables for user solution\n");
        for (Parameter param : signature.getParameters()) {
            String varName = param.getName() + testNumber + "User";
            Object value = inputs.get(param.getName());
            String javaCode = convertToJavaCode(value, param.getType());
            
            code.append("            ")
                .append(param.getType())
                .append(" ")
                .append(varName)
                .append(" = ")
                .append(javaCode)
                .append(";\n");
        }
        
        // ✅ DEEP COPY for user solution if needed
        for (Parameter param : signature.getParameters()) {
            if (param.getType().equals("int[][]")) {
                String varName = param.getName() + testNumber + "User";
                code.append("            // Deep copy for user solution\n");
                code.append("            int[][] ").append(varName).append("Copy = new int[")
                    .append(varName).append(".length][];\n");
                code.append("            for (int i = 0; i < ").append(varName).append(".length; i++) {\n");
                code.append("                ").append(varName).append("Copy[i] = java.util.Arrays.copyOf(")
                    .append(varName).append("[i], ").append(varName).append("[i].length);\n");
                code.append("            }\n");
            } else if (param.getType().equals("int[]")) {
                String varName = param.getName() + testNumber + "User";
                code.append("            // Deep copy for user solution\n");
                code.append("            int[] ").append(varName).append("Copy = java.util.Arrays.copyOf(")
                    .append(varName).append(", ").append(varName).append(".length);\n");
            }
        }
        
        code.append("\n");
        
        // Call CORRECT solution
        code.append("            ")
            .append(signature.getReturnType())
            .append(" expected = correctSolution").append(testNumber).append(".")
            .append(signature.getMethodName())
            .append("(");
        
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName() + testNumber + "Correct";
            String paramType = signature.getParameters().get(i).getType();
            if (paramType.equals("int[][]") || paramType.equals("int[]")) {
                paramName += "Copy";
            }
            code.append(paramName);
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n\n");
        
        // Call USER solution
        code.append("            ")
            .append(signature.getReturnType())
            .append(" actual = userSolution").append(testNumber).append(".")
            .append(signature.getMethodName())
            .append("(");
        
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName() + testNumber + "User";
            String paramType = signature.getParameters().get(i).getType();
            if (paramType.equals("int[][]") || paramType.equals("int[]")) {
                paramName += "Copy";
            }
            code.append(paramName);
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n\n");
        
        // Print results
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"EXPECTED_OUTPUT : \" + expected);\n");
        code.append("            System.out.println(\"USER_OUTPUT : \" + actual);\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n\n");
        
        // Exception handling
        code.append("        } catch (Exception e) {\n");
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"ERROR : \" + e.getMessage());\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n");
        code.append("        }\n\n");
        
        return code.toString();
    }

    private String convertToJavaCode(Object value, String type) {
        if (value == null) return "null";
        
        if (type.equals("int[][]")) {
            return convert2DIntArray((List<?>) value);
        }
        if (type.equals("int[]")) {
            return convert1DIntArray((List<?>) value);
        }
        if (type.equals("String[]")) {
            return convertStringArray((List<?>) value);
        }
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
        return String.valueOf(value);
    }

    private String convert2DIntArray(List<?> array) {
        if (array == null || array.isEmpty()) return "new int[0][0]";
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
        if (array == null || array.isEmpty()) return "new int[0]";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convertStringArray(List<?> array) {
        if (array == null || array.isEmpty()) return "new String[0]";
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