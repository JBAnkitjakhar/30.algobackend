// src/main/java/com/algoarena/service/compiler/submitmode/JavaSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaSubmitTemplateGenerator {

    public String generateSubmitTemplate(
            String userCode,
            List<Question.Testcase> testcases,
            String methodName) {

        MethodSignature signature = extractMethodSignature(userCode, methodName);

        StringBuilder template = new StringBuilder();

        template.append("import java.util.*;\n\n");
        template.append(generateMainClass(signature, testcases));
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n");

        return template.toString();
    }

    private MethodSignature extractMethodSignature(String userCode, String methodName) {
        String patternString = "public\\s+(\\w+(?:\\[\\])*?)\\s+" +
                Pattern.quote(methodName) +
                "\\s*\\(([^)]*)\\)";

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(userCode);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Could not find method '" + methodName + "' in user code");
        }

        String returnType = matcher.group(1).trim();
        String paramsString = matcher.group(2).trim();

        List<Parameter> parameters = new ArrayList<>();

        if (!paramsString.isEmpty()) {
            String[] paramPairs = paramsString.split(",");
            for (String pair : paramPairs) {
                String[] parts = pair.trim().split("\\s+");
                if (parts.length >= 2) {
                    parameters.add(new Parameter(parts[0], parts[1]));
                }
            }
        }

        return new MethodSignature(returnType, methodName, parameters);
    }

    private String generateMainClass(MethodSignature signature, List<Question.Testcase> testcases) {
        StringBuilder main = new StringBuilder();

        main.append("public class Main {\n");
        main.append("    public static void main(String[] args) {\n");
        // main.append(" Solution solution = new Solution();\n\n");

        // Generate each test case with timing
        for (Question.Testcase testcase : testcases) {
            main.append(generateTestCase(testcase, signature));
        }

        main.append("    }\n");
        main.append("}\n");

        return main.toString();
    }

    /**
     * ✅ UPDATED: Add timing measurement per testcase
     */
    private String generateTestCase(Question.Testcase testcase, MethodSignature signature) {
        StringBuilder code = new StringBuilder();

        int testNumber = testcase.getId();
        code.append("        // ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("        try {\n");

        // CREATE FRESH SOLUTION INSTANCE
        code.append("            Solution solution").append(testNumber).append(" = new Solution();\n");

        Map<String, Object> inputs = testcase.getInput();
        List<String> varNames = new ArrayList<>();

        // Generate variables WITH DEEP COPY FOR ARRAYS
        for (Parameter param : signature.getParameters()) {
            String varName = param.getName() + testNumber;
            varNames.add(varName);

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

        // ✅ ADD DEEP COPY FOR 2D ARRAYS (if grid parameter exists)
        for (Parameter param : signature.getParameters()) {
            if (param.getType().equals("int[][]")) {
                String varName = param.getName() + testNumber;
                code.append("            // Deep copy to prevent array pollution\n");
                code.append("            ")
                        .append(param.getType())
                        .append(" ")
                        .append(varName)
                        .append("Copy = new int[")
                        .append(varName)
                        .append(".length][];\n");
                code.append("            for (int i = 0; i < ")
                        .append(varName)
                        .append(".length; i++) {\n");
                code.append("                ")
                        .append(varName)
                        .append("Copy[i] = java.util.Arrays.copyOf(")
                        .append(varName)
                        .append("[i], ")
                        .append(varName)
                        .append("[i].length);\n");
                code.append("            }\n");

                // Update varNames to use the copied version
                varNames.set(varNames.indexOf(varName), varName + "Copy");
            }
        }

        // START TIMING
        code.append("            long startTime").append(testNumber).append(" = System.nanoTime();\n");

        // CALL USER SOLUTION
        code.append("            ")
                .append(signature.getReturnType())
                .append(" result")
                .append(testNumber)
                .append(" = solution").append(testNumber).append(".")
                .append(signature.getMethodName())
                .append("(");

        for (int i = 0; i < varNames.size(); i++) {
            code.append(varNames.get(i));
            if (i < varNames.size() - 1)
                code.append(", ");
        }

        code.append(");\n");

        // END TIMING
        code.append("            long endTime").append(testNumber).append(" = System.nanoTime();\n");
        code.append("            long executionTime").append(testNumber)
                .append(" = (endTime").append(testNumber).append(" - startTime").append(testNumber)
                .append(") / 1000000;\n");

        // Print output
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"TEST_CASE_ID : ").append(testNumber).append("\");\n");
        code.append("            System.out.println(\"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput())
                .append("\");\n");
        code.append("            System.out.println(\"USER_OUTPUT : \" + result").append(testNumber).append(");\n");
        code.append("            System.out.println(\"EXECUTION_TIME : \" + executionTime").append(testNumber)
                .append(");\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n");

        // Exception handling
        code.append("        } catch (Exception e) {\n");
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"TEST_CASE_ID : ").append(testNumber).append("\");\n");
        code.append("            System.out.println(\"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput())
                .append("\");\n");
        code.append("            System.out.println(\"ERROR : \" + e.getMessage());\n");
        code.append("            System.out.println(\"EXECUTION_TIME : 0\");\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n");
        code.append("        }\n\n");

        return code.toString();
    }

    private String convertToJavaCode(Object value, String type) {
        if (value == null)
            return "null";

        if (type.equals("int[][]"))
            return convert2DIntArray((List<?>) value);
        if (type.equals("int[]"))
            return convert1DIntArray((List<?>) value);
        if (type.equals("String[]"))
            return convertStringArray((List<?>) value);

        if (type.equals("int") || type.equals("long") || type.equals("double") || type.equals("float")) {
            return String.valueOf(value);
        }

        if (type.equals("boolean"))
            return String.valueOf(value);
        if (type.equals("char"))
            return "'" + value + "'";
        if (type.equals("String"))
            return "\"" + escapeString(String.valueOf(value)) + "\"";

        return String.valueOf(value);
    }

    private String convert2DIntArray(List<?> array) {
        if (array == null || array.isEmpty())
            return "new int[0][0]";

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            List<?> row = (List<?>) array.get(i);
            sb.append("{");
            for (int j = 0; j < row.size(); j++) {
                sb.append(row.get(j));
                if (j < row.size() - 1)
                    sb.append(",");
            }
            sb.append("}");
            if (i < array.size() - 1)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DIntArray(List<?> array) {
        if (array == null || array.isEmpty())
            return "new int[0]";

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convertStringArray(List<?> array) {
        if (array == null || array.isEmpty())
            return "new String[0]";

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append("\"").append(escapeString(String.valueOf(array.get(i)))).append("\"");
            if (i < array.size() - 1)
                sb.append(",");
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

        public String getReturnType() {
            return returnType;
        }

        public String getMethodName() {
            return methodName;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }
    }

    public static class Parameter {
        private final String type;
        private final String name;

        public Parameter(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}