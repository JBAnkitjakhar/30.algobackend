// src/main/java/com/algoarena/service/compiler/submitmode/CppSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CppSubmitTemplateGenerator {

    public String generateSubmitTemplate(
            String userCode,
            List<Question.Testcase> testcases,
            String methodName) {
        
        MethodSignature signature = extractMethodSignature(userCode, methodName);
        
        StringBuilder template = new StringBuilder();
        
        // Includes
        template.append("#include <iostream>\n");
        template.append("#include <vector>\n");
        template.append("#include <string>\n");
        template.append("#include <set>\n");
        template.append("#include <map>\n");
        template.append("#include <algorithm>\n");
        template.append("#include <chrono>\n");
        template.append("using namespace std;\n");
        template.append("using namespace std::chrono;\n\n");
        
        // User solution
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");
        
        // Main function
        template.append(generateMainFunction(signature, testcases));
        
        return template.toString();
    }

    private MethodSignature extractMethodSignature(String userCode, String methodName) {
        String patternString = "(\\w+(?:<[^>]+>)?(?:\\s*&)?(?:\\s*\\*)?(?:\\[\\])?(?:&)?)\\s+" + 
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
                pair = pair.trim();
                int lastSpace = pair.lastIndexOf(' ');
                if (lastSpace > 0) {
                    String type = pair.substring(0, lastSpace).trim();
                    String name = pair.substring(lastSpace + 1).trim();
                    name = name.replaceAll("[&*]", "");
                    parameters.add(new Parameter(type, name));
                }
            }
        }

        return new MethodSignature(returnType, methodName, parameters);
    }

    private String generateMainFunction(MethodSignature signature, List<Question.Testcase> testcases) {
        StringBuilder main = new StringBuilder();

        main.append("int main() {\n");

        for (Question.Testcase testcase : testcases) {
            main.append(generateTestCase(testcase, signature));
        }

        main.append("    return 0;\n");
        main.append("}\n");

        return main.toString();
    }

    private String generateTestCase(Question.Testcase testcase, MethodSignature signature) {
        StringBuilder code = new StringBuilder();

        int testNumber = testcase.getId();
        code.append("    // ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("    {\n");

        Map<String, Object> inputs = testcase.getInput();

        // Generate variables with deep copy
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String cppCode = convertToCppCode(value, param.getType());
            String declType = param.getType().replaceAll("&|\\*", "").trim();

            code.append("        ")
                    .append(declType)
                    .append(" ")
                    .append(param.getName())
                    .append(testNumber)
                    .append(" = ")
                    .append(cppCode)
                    .append(";\n");
        }

        // Deep copy for fresh instance isolation
        for (Parameter param : signature.getParameters()) {
            if (param.getType().contains("vector")) {
                String declType = param.getType().replaceAll("&|\\*", "").trim();
                code.append("        ")
                        .append(declType)
                        .append(" ")
                        .append(param.getName())
                        .append(testNumber)
                        .append("Copy = ")
                        .append(param.getName())
                        .append(testNumber)
                        .append(";\n");
            }
        }

        code.append("\n");
        code.append("        try {\n");
        code.append("            Solution solution").append(testNumber).append(";\n");
        code.append("            auto start = high_resolution_clock::now();\n");
        code.append("            ")
                .append(signature.getReturnType())
                .append(" result").append(testNumber)
                .append(" = solution").append(testNumber).append(".")
                .append(signature.getMethodName())
                .append("(");

        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName() + testNumber;
            if (signature.getParameters().get(i).getType().contains("vector")) {
                paramName += "Copy";
            }
            code.append(paramName);
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }

        code.append(");\n");
        code.append("            auto end = high_resolution_clock::now();\n");
        code.append("            auto duration = duration_cast<milliseconds>(end - start).count();\n\n");

        code.append("            cout << \"TEST_CASE_START\" << endl;\n");
        code.append("            cout << \"TEST_CASE_ID : ").append(testNumber).append("\" << endl;\n");
        code.append("            cout << \"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("\" << endl;\n");
        code.append("            cout << \"USER_OUTPUT : \" << result").append(testNumber).append(" << endl;\n");
        code.append("            cout << \"EXECUTION_TIME : \" << duration << endl;\n");
        code.append("            cout << \"TEST_CASE_END\" << endl;\n");

        code.append("        } catch (const exception& e) {\n");
        code.append("            cout << \"TEST_CASE_START\" << endl;\n");
        code.append("            cout << \"TEST_CASE_ID : ").append(testNumber).append("\" << endl;\n");
        code.append("            cout << \"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("\" << endl;\n");
        code.append("            cout << \"ERROR : \" << e.what() << endl;\n");
        code.append("            cout << \"EXECUTION_TIME : 0\" << endl;\n");
        code.append("            cout << \"TEST_CASE_END\" << endl;\n");
        code.append("        }\n");

        code.append("    }\n\n");

        return code.toString();
    }

    private String convertToCppCode(Object value, String type) {
        if (value == null) return "{}";
        
        if (type.contains("vector<vector<int>>")) {
            return convert2DIntVector((List<?>) value);
        }
        if (type.contains("vector<int>")) {
            return convert1DIntVector((List<?>) value);
        }
        if (type.contains("vector<string>")) {
            return convertStringVector((List<?>) value);
        }
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
        if (array == null || array.isEmpty()) return "vector<vector<int>>()";
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

    private String convert1DIntVector(List<?> array) {
        if (array == null || array.isEmpty()) return "vector<int>()";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convertStringVector(List<?> array) {
        if (array == null || array.isEmpty()) return "vector<string>()";
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