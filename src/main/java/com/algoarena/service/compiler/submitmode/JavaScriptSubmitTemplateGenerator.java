// src/main/java/com/algoarena/service/compiler/submitmode/JavaScriptSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaScriptSubmitTemplateGenerator {

    public String generateSubmitTemplate(
            String userCode,
            List<Question.Testcase> testcases,
            String methodName) {
        
        MethodSignature signature = extractMethodSignature(userCode, methodName);
        
        StringBuilder template = new StringBuilder();
        
        // User solution
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");
        
        // Main execution
        template.append(generateMainExecution(signature, testcases));
        
        return template.toString();
    }

    private MethodSignature extractMethodSignature(String userCode, String methodName) {
        String patternString = "Solution\\.prototype\\." + 
                               Pattern.quote(methodName) + 
                               "\\s*=\\s*function\\s*\\(([^)]*)\\)";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(userCode);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Could not find method '" + methodName + "' in user code");
        }
        
        String paramsString = matcher.group(1).trim();
        
        List<Parameter> parameters = new ArrayList<>();
        
        if (!paramsString.isEmpty()) {
            String[] paramNames = paramsString.split(",");
            for (String name : paramNames) {
                parameters.add(new Parameter("any", name.trim()));
            }
        }
        
        return new MethodSignature("any", methodName, parameters);
    }

    private String generateMainExecution(MethodSignature signature, List<Question.Testcase> testcases) {
        StringBuilder main = new StringBuilder();
        
        for (Question.Testcase testcase : testcases) {
            main.append(generateTestCase(testcase, signature));
        }
        
        return main.toString();
    }

    private String generateTestCase(Question.Testcase testcase, MethodSignature signature) {
        StringBuilder code = new StringBuilder();
        
        int testNumber = testcase.getId();
        code.append("// ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("{\n");
        
        Map<String, Object> inputs = testcase.getInput();
        
        // Generate variables
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String jsCode = convertToJsCode(value);
            code.append("    const ").append(param.getName()).append(testNumber)
                .append(" = ").append(jsCode).append(";\n");
        }
        
        code.append("\n");
        code.append("    try {\n");
        code.append("        const solution").append(testNumber).append(" = new Solution();\n");
        
        // Deep copy parameters
        for (Parameter param : signature.getParameters()) {
            code.append("        const ").append(param.getName()).append(testNumber)
                .append("Copy = JSON.parse(JSON.stringify(")
                .append(param.getName()).append(testNumber).append("));\n");
        }
        
        code.append("        const startTime = Date.now();\n");
        code.append("        const result").append(testNumber).append(" = solution").append(testNumber)
            .append(".").append(signature.getMethodName()).append("(");
        
        for (int i = 0; i < signature.getParameters().size(); i++) {
            code.append(signature.getParameters().get(i).getName()).append(testNumber).append("Copy");
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        
        code.append(");\n");
        code.append("        const endTime = Date.now();\n");
        code.append("        const executionTime = endTime - startTime;\n\n");
        
        code.append("        console.log(\"TEST_CASE_START\");\n");
        code.append("        console.log(`TEST_CASE_ID : ").append(testNumber).append("`);\n");
        code.append("        console.log(`EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("`);\n");
        code.append("        console.log(`USER_OUTPUT : ${result").append(testNumber)
            .append(" !== null && result").append(testNumber).append(" !== undefined ? result")
            .append(testNumber).append(" : 'null'}`);\n");
        code.append("        console.log(`EXECUTION_TIME : ${executionTime}`);\n");
        code.append("        console.log(\"TEST_CASE_END\");\n");
        
        code.append("    } catch (e) {\n");
        code.append("        console.log(\"TEST_CASE_START\");\n");
        code.append("        console.log(`TEST_CASE_ID : ").append(testNumber).append("`);\n");
        code.append("        console.log(`EXPECTED_OUTPUT : ").append(testcase.getExpectedOutput()).append("`);\n");
        code.append("        console.log(`ERROR : ${e.message}`);\n");
        code.append("        console.log(\"EXECUTION_TIME : 0\");\n");
        code.append("        console.log(\"TEST_CASE_END\");\n");
        code.append("    }\n");
        code.append("}\n\n");
        
        return code.toString();
    }

    private String convertToJsCode(Object value) {
        if (value == null) return "null";
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            
            if (list.get(0) instanceof List) {
                return convert2DArray(list);
            } else {
                return convert1DArray(list);
            }
        }
        
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        
        if (value instanceof Boolean) {
            return value.toString();
        }
        
        return String.valueOf(value);
    }

    private String convert2DArray(List<?> array) {
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

    private String convert1DArray(List<?> array) {
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