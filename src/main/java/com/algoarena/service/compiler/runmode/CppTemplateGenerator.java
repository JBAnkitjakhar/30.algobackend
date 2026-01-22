// src/main/java/com/algoarena/service/compiler/runmode/CppTemplateGenerator.java

package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CppTemplateGenerator {

    /**
     * Generates complete C++ code for execution
     */
    public String generateRunTemplate(
            String correctSolution,
            String userCode,
            List<RunTestCaseInput> testCases,
            String methodName) { // ✅ NEW parameter

        // ✅ Extract method signature using the provided methodName
        MethodSignature signature = extractMethodSignature(correctSolution, methodName);

        StringBuilder template = new StringBuilder();

        // 1. Includes
        template.append("#include <iostream>\n");
        template.append("#include <vector>\n");
        template.append("#include <string>\n");
        template.append("#include <set>\n");
        template.append("#include <map>\n");
        template.append("#include <algorithm>\n");
        template.append("using namespace std;\n\n");

        // 2. Correct solution (rename Solution -> CorrectSolution)
        template.append("// ===== CORRECT SOLUTION =====\n");
        template.append(correctSolution.replace("class Solution", "class CorrectSolution"));
        template.append("\n\n");

        // 3. User solution
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");

        // 4. Main function with test cases
        template.append(generateMainFunction(signature, testCases));

        return template.toString();
    }

    /**
     * ✅ UPDATED: Extract method signature for the SPECIFIC methodName
     */
    private MethodSignature extractMethodSignature(String correctSolution, String methodName) {
        // Pattern: returnType METHODNAME(params)
        // More flexible pattern to handle complex return types
        String patternString = "(\\w+(?:<[^>]+>)?(?:\\s*&)?(?:\\s*\\*)?(?:\\[\\])?(?:&)?)\\s+" + 
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
                // Extract type and name from "vector<vector<int>>& grid"
                pair = pair.trim();
                int lastSpace = pair.lastIndexOf(' ');
                if (lastSpace > 0) {
                    String type = pair.substring(0, lastSpace).trim();
                    String name = pair.substring(lastSpace + 1).trim();
                    // Remove & or * from name if present
                    name = name.replaceAll("[&*]", "");
                    parameters.add(new Parameter(type, name));
                }
            }
        }

        return new MethodSignature(returnType, methodName, parameters);
    }

    /**
     * Generate main function with all test cases
     */
    private String generateMainFunction(MethodSignature signature, List<RunTestCaseInput> testCases) {
        StringBuilder main = new StringBuilder();

        main.append("int main() {\n");
        main.append("    CorrectSolution correctSolution;\n");
        main.append("    Solution userSolution;\n\n");

        // Generate each test case
        for (int i = 0; i < testCases.size(); i++) {
            main.append(generateTestCase(testCases.get(i), i + 1, signature));
        }

        main.append("    return 0;\n");
        main.append("}\n");

        return main.toString();
    }

    /**
     * Generate single test case code
     */
    private String generateTestCase(RunTestCaseInput testCase, int testNumber, MethodSignature signature) {
        StringBuilder code = new StringBuilder();

        code.append("    // ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("    {\n");

        Map<String, Object> inputs = testCase.getInput();

        // Generate variables for each parameter
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String cppCode = convertToCppCode(value, param.getType());

            // Remove reference/pointer from type for variable declaration
            String declType = param.getType().replaceAll("&|\\*", "").trim();

            code.append("        ")
                    .append(declType)
                    .append(" ")
                    .append(param.getName())
                    .append(" = ")
                    .append(cppCode)
                    .append(";\n");
        }

        code.append("\n");
        code.append("        try {\n");

        // Call correct solution
        code.append("            ").append(signature.getReturnType())
                .append(" expected = correctSolution.")
                .append(signature.getMethodName())
                .append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            code.append(signature.getParameters().get(i).getName());
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n");

        // Call user solution
        code.append("            ").append(signature.getReturnType())
                .append(" actual = userSolution.")
                .append(signature.getMethodName())
                .append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            code.append(signature.getParameters().get(i).getName());
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n\n");

        // Print results
        code.append("            cout << \"TEST_CASE_START\" << endl;\n");
        code.append("            cout << \"EXPECTED_OUTPUT : \" << expected << endl;\n");
        code.append("            cout << \"USER_OUTPUT : \" << actual << endl;\n");
        code.append("            cout << \"TEST_CASE_END\" << endl;\n");

        code.append("        } catch (const exception& e) {\n");
        code.append("            cout << \"TEST_CASE_START\" << endl;\n");
        code.append("            cout << \"ERROR : \" << e.what() << endl;\n");
        code.append("            cout << \"TEST_CASE_END\" << endl;\n");
        code.append("        }\n");

        code.append("    }\n\n");

        return code.toString();
    }

    /**
     * Convert JSON value to C++ code string
     */
    private String convertToCppCode(Object value, String type) {
        if (value == null) {
            return "{}";
        }

        // Handle vector<vector<int>>
        if (type.contains("vector<vector<int>>")) {
            return convert2DIntVector((List<?>) value);
        }

        // Handle vector<int>
        if (type.contains("vector<int>")) {
            return convert1DIntVector((List<?>) value);
        }

        // Handle vector<string>
        if (type.contains("vector<string>")) {
            return convertStringVector((List<?>) value);
        }

        // Handle primitives
        if (type.equals("int") || type.equals("long") || type.equals("double") || type.equals("float")) {
            return String.valueOf(value);
        }

        if (type.equals("bool")) {
            return String.valueOf(value).toLowerCase();
        }

        if (type.equals("char")) {
            return "'" + value + "'";
        }

        if (type.equals("string")) {
            return "\"" + escapeString(String.valueOf(value)) + "\"";
        }

        return String.valueOf(value);
    }

    private String convert2DIntVector(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "vector<vector<int>>()";
        }

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

    private String convert1DIntVector(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "vector<int>()";
        }

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convertStringVector(List<?> array) {
        if (array == null || array.isEmpty()) {
            return "vector<string>()";
        }

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