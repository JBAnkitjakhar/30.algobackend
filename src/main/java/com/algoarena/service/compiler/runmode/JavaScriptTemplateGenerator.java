// src/main/java/com/algoarena/service/compiler/runmode/JavaScriptTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaScriptTemplateGenerator {

    /**
     * Generates complete JavaScript code for execution
     */
    public String generateRunTemplate(
            String correctSolution,
            String userCode,
            List<RunTestCaseInput> testCases,
            String methodName) { // ✅ NEW parameter
        
        // ✅ Extract method signature using the provided methodName
        MethodSignature signature = extractMethodSignature(correctSolution, methodName);
        
        StringBuilder template = new StringBuilder();
        
        // 1. Correct solution (rename Solution -> CorrectSolution)
        template.append("// ===== CORRECT SOLUTION =====\n");
        String renamedCorrectSolution = correctSolution
            .replace("var Solution = function()", "var CorrectSolution = function()")
            .replace("Solution.prototype.", "CorrectSolution.prototype.");
        template.append(renamedCorrectSolution);
        template.append("\n\n");
        
        // 2. User solution
        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n\n");
        
        // 3. Main execution with test cases
        template.append(generateMainExecution(signature, testCases));
        
        return template.toString();
    }

    /**
     * ✅ UPDATED: Extract method signature for the SPECIFIC methodName
     */
    private MethodSignature extractMethodSignature(String correctSolution, String methodName) {
        // Pattern: Solution.prototype.METHODNAME = function(params)
        String patternString = "Solution\\.prototype\\." + 
                               Pattern.quote(methodName) + 
                               "\\s*=\\s*function\\s*\\(([^)]*)\\)";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(correctSolution);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Could not find method '" + methodName + "' in correct solution");
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

    /**
     * Generate main execution with all test cases
     */
    private String generateMainExecution(MethodSignature signature, List<RunTestCaseInput> testCases) {
        StringBuilder main = new StringBuilder();
        
        main.append("const correctSolution = new CorrectSolution();\n");
        main.append("const userSolution = new Solution();\n\n");
        
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
        
        code.append("// ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("{\n");
        
        Map<String, Object> inputs = testCase.getInput();
        
        // Generate variables
        for (Parameter param : signature.getParameters()) {
            Object value = inputs.get(param.getName());
            String jsCode = convertToJsCode(value);
            code.append("    const ").append(param.getName()).append(" = ").append(jsCode).append(";\n");
        }
        
        code.append("\n");
        code.append("    try {\n");
        
        // Deep copy for correct solution parameters (to avoid mutation)
        code.append("        const expected = correctSolution.").append(signature.getMethodName()).append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName();
            code.append("JSON.parse(JSON.stringify(").append(paramName).append("))");
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n");
        
        // Deep copy for user solution parameters (to avoid mutation)
        code.append("        const actual = userSolution.").append(signature.getMethodName()).append("(");
        for (int i = 0; i < signature.getParameters().size(); i++) {
            String paramName = signature.getParameters().get(i).getName();
            code.append("JSON.parse(JSON.stringify(").append(paramName).append("))");
            if (i < signature.getParameters().size() - 1) {
                code.append(", ");
            }
        }
        code.append(");\n\n");
        
        // Print results - handle null/undefined returns
        code.append("        console.log(\"TEST_CASE_START\");\n");
        code.append("        console.log(`EXPECTED_OUTPUT : ${expected !== null && expected !== undefined ? expected : 'null'}`);\n");
        code.append("        console.log(`USER_OUTPUT : ${actual !== null && actual !== undefined ? actual : 'null'}`);\n");
        code.append("        console.log(\"TEST_CASE_END\");\n");
        
        code.append("    } catch (e) {\n");
        code.append("        console.log(\"TEST_CASE_START\");\n");
        code.append("        console.log(`ERROR : ${e.message}`);\n");
        code.append("        console.log(\"TEST_CASE_END\");\n");
        code.append("    }\n");
        code.append("}\n\n");
        
        return code.toString();
    }

    /**
     * Convert JSON value to JavaScript code string
     */
    private String convertToJsCode(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            
            // Check if it's a 2D array
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