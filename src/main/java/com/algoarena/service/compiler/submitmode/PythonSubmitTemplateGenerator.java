// src/main/java/com/algoarena/service/compiler/submitmode/PythonSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PythonSubmitTemplateGenerator {

    public String generateSubmitTemplate(
            String userCode,
            List<Question.Testcase> testcases,
            String methodName) {
        
        MethodSignature signature = extractMethodSignature(userCode, methodName);
        
        StringBuilder template = new StringBuilder();
        
        // Imports
        template.append("from typing import List, Optional\n");
        template.append("import sys\n");
        template.append("import copy\n");
        template.append("import time\n\n");
        
        // User solution
        template.append("# ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");
        
        // Main execution
        template.append(generateMainExecution(signature, testcases));
        
        return template.toString();
    }

    private MethodSignature extractMethodSignature(String userCode, String methodName) {
        String patternString = "def\\s+" + 
                               Pattern.quote(methodName) + 
                               "\\s*\\(self(?:,\\s*([^)]*))?\\)(?:\\s*->\\s*(\\w+(?:\\[.*?\\])?))?\\s*:";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(userCode);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Could not find method '" + methodName + "' in user code");
        }
        
        String paramsString = matcher.group(1);
        String returnType = matcher.group(2) != null ? matcher.group(2) : "None";
        
        List<Parameter> parameters = new ArrayList<>();
        
        if (paramsString != null && !paramsString.trim().isEmpty()) {
            String[] paramPairs = paramsString.split(",");
            for (String pair : paramPairs) {
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

    private String generateMainExecution(MethodSignature signature, List<Question.Testcase> testcases) {
        StringBuilder main = new StringBuilder();
        
        main.append("if __name__ == \"__main__\":\n");
        
        for (Question.Testcase testcase : testcases) {
            main.append(generateTestCase(testcase, signature));
        }
        
        return main.toString();
    }

    private String generateTestCase(Question.Testcase testcase, MethodSignature signature) {
        StringBuilder code = new StringBuilder();
        
        int testNumber = testcase.getId();
        code.append("    # ===== TEST CASE ").append(testNumber).append(" =====\n");
        
        Map<String, Object> inputs = testcase.getInput();
        
        // Generate variables
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String pythonCode = convertToPythonCode(value);
            code.append("    ").append(param.getName()).append(testNumber).append(" = ").append(pythonCode).append("\n");
        }
        
        code.append("\n");
        code.append("    try:\n");
        code.append("        solution").append(testNumber).append(" = Solution()\n");
        
        // Deep copy parameters
        for (Parameter param : signature.getParameters()) {
            if (param.getType().contains("List")) {
                code.append("        ").append(param.getName()).append(testNumber)
                    .append("Copy = copy.deepcopy(").append(param.getName()).append(testNumber).append(")\n");
            }
        }
        
        code.append("        start_time = time.time()\n");
        code.append("        result").append(testNumber).append(" = solution").append(testNumber)
            .append(".").append(signature.getMethodName()).append("(");
        
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName() + testNumber;
            String paramType = signature.getParameters().get(i).getType();
            
            if (paramType.contains("List")) {
                paramName += "Copy";
            }
            
            code.append(paramName);
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        
        code.append(")\n");
        code.append("        end_time = time.time()\n");
        code.append("        execution_time = int((end_time - start_time) * 1000)\n\n");
        
        code.append("        print(\"TEST_CASE_START\")\n");
        code.append("        print(f\"TEST_CASE_ID : ").append(testNumber).append("\")\n");
        code.append("        print(f\"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("\")\n");
        code.append("        print(f\"USER_OUTPUT : {result").append(testNumber).append(" if result")
            .append(testNumber).append(" is not None else 'None'}\")\n");
        code.append("        print(f\"EXECUTION_TIME : {execution_time}\")\n");
        code.append("        print(\"TEST_CASE_END\")\n");
        
        code.append("    except Exception as e:\n");
        code.append("        print(\"TEST_CASE_START\")\n");
        code.append("        print(f\"TEST_CASE_ID : ").append(testNumber).append("\")\n");
        code.append("        print(f\"EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("\")\n");
        code.append("        print(f\"ERROR : {str(e)}\")\n");
        code.append("        print(\"EXECUTION_TIME : 0\")\n");
        code.append("        print(\"TEST_CASE_END\")\n\n");
        
        return code.toString();
    }

    private String convertToPythonCode(Object value) {
        if (value == null) return "None";
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            
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