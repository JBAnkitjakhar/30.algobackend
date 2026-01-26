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

        // Imports - comprehensive for DSA problems
        template.append("import java.util.*;\n");
        template.append("import java.util.stream.*;\n\n");

        // Main class MUST come first for Piston to find main()
        template.append(generateMainClass(signature, testcases));

        // Helper class for output formatting (after Main)
        template.append(generateOutputHelper());
        template.append("\n");

        template.append("// ===== USER SOLUTION =====\n");
        template.append(userCode);
        template.append("\n");

        return template.toString();
    }

    /**
     * Helper class to format outputs consistently for comparison
     */
    private String generateOutputHelper() {
        return """
class OutputHelper {
    public static String format(Object obj) {
        if (obj == null) return "null";

        // Handle primitive arrays
        if (obj instanceof int[]) {
            return Arrays.toString((int[]) obj);
        }
        if (obj instanceof int[][]) {
            return Arrays.deepToString((int[][]) obj);
        }
        if (obj instanceof long[]) {
            return Arrays.toString((long[]) obj);
        }
        if (obj instanceof double[]) {
            return Arrays.toString((double[]) obj);
        }
        if (obj instanceof float[]) {
            return Arrays.toString((float[]) obj);
        }
        if (obj instanceof boolean[]) {
            return Arrays.toString((boolean[]) obj);
        }
        if (obj instanceof char[]) {
            return Arrays.toString((char[]) obj);
        }
        if (obj instanceof String[]) {
            return Arrays.toString((String[]) obj);
        }
        if (obj instanceof Object[]) {
            return Arrays.deepToString((Object[]) obj);
        }

        // Handle List types (including nested)
        if (obj instanceof List) {
            return formatList((List<?>) obj);
        }

        // Handle other objects
        return String.valueOf(obj);
    }

    private static String formatList(List<?> list) {
        if (list == null) return "null";
        if (list.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof List) {
                sb.append(formatList((List<?>) item));
            } else {
                sb.append(item);
            }
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
""";
    }

    /**
     * Extract method signature with support for generic types like List<List<Integer>>
     */
    private MethodSignature extractMethodSignature(String userCode, String methodName) {
        // Pattern to match: public ReturnType methodName(params)
        // ReturnType can be: int, int[], List<Integer>, List<List<Integer>>, etc.
        String patternString = "public\\s+(" +
                "(?:List|Set|Map|Queue|Deque|Stack|TreeNode|ListNode|Optional)" +
                "(?:<[^>]+(?:<[^>]+>)?[^>]*>)?" +  // Generic types with possible nesting
                "|\\w+(?:\\[\\])*" +                // Primitive types and arrays
                ")\\s+" +
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

        List<Parameter> parameters = parseParameters(paramsString);

        return new MethodSignature(returnType, methodName, parameters);
    }

    /**
     * Parse method parameters, handling generic types
     */
    private List<Parameter> parseParameters(String paramsString) {
        List<Parameter> parameters = new ArrayList<>();

        if (paramsString == null || paramsString.isEmpty()) {
            return parameters;
        }

        // Split by comma, but respect generic angle brackets
        List<String> paramPairs = splitParameters(paramsString);

        for (String pair : paramPairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // Find the last space that separates type from name
            int lastSpace = findTypeNameSeparator(pair);

            if (lastSpace > 0) {
                String type = pair.substring(0, lastSpace).trim();
                String name = pair.substring(lastSpace + 1).trim();
                parameters.add(new Parameter(type, name));
            }
        }

        return parameters;
    }

    /**
     * Split parameters respecting generic brackets
     */
    private List<String> splitParameters(String paramsString) {
        List<String> params = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (char c : paramsString.toCharArray()) {
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                params.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }

        if (current.length() > 0) {
            params.add(current.toString());
        }

        return params;
    }

    /**
     * Find the separator between type and parameter name
     */
    private int findTypeNameSeparator(String pair) {
        int depth = 0;
        int lastSpace = -1;

        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ' ' && depth == 0) {
                lastSpace = i;
            }
        }

        return lastSpace;
    }

    private String generateMainClass(MethodSignature signature, List<Question.Testcase> testcases) {
        StringBuilder main = new StringBuilder();

        main.append("public class Main {\n");
        main.append("    public static void main(String[] args) {\n");

        // Generate each test case with timing
        for (Question.Testcase testcase : testcases) {
            main.append(generateTestCase(testcase, signature));
        }

        main.append("    }\n");
        main.append("}\n");

        return main.toString();
    }

    private String generateTestCase(Question.Testcase testcase, MethodSignature signature) {
        StringBuilder code = new StringBuilder();

        int testNumber = testcase.getId();
        code.append("        // ===== TEST CASE ").append(testNumber).append(" =====\n");
        code.append("        try {\n");

        // CREATE FRESH SOLUTION INSTANCE
        code.append("            Solution solution").append(testNumber).append(" = new Solution();\n");

        Map<String, Object> inputs = testcase.getInput();
        List<String> varNames = new ArrayList<>();

        // Generate variables with deep copy for arrays/lists
        for (Parameter param : signature.getParameters()) {
            String varName = param.getName() + testNumber;
            varNames.add(varName);

            Object value = inputs.get(param.getName());
            String javaCode = convertToJavaCode(value, param.getType());

            // Apply deep copy based on type
            String finalValue = deepCopyExpression(javaCode, param.getType());

            code.append("            ")
                    .append(param.getType())
                    .append(" ")
                    .append(varName)
                    .append(" = ")
                    .append(finalValue)
                    .append(";\n");
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

        // Format expected output for comparison
        String expectedOutputFormatted = formatExpectedOutput(testcase.getExpectedOutput());

        // Print output using OutputHelper for proper formatting
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"TEST_CASE_ID : ").append(testNumber).append("\");\n");
        code.append("            System.out.println(\"EXPECTED_OUTPUT : ").append(expectedOutputFormatted).append("\");\n");
        code.append("            System.out.println(\"USER_OUTPUT : \" + OutputHelper.format(result").append(testNumber).append("));\n");
        code.append("            System.out.println(\"EXECUTION_TIME : \" + executionTime").append(testNumber).append(");\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n");

        // Exception handling
        code.append("        } catch (Exception e) {\n");
        code.append("            System.out.println(\"TEST_CASE_START\");\n");
        code.append("            System.out.println(\"TEST_CASE_ID : ").append(testNumber).append("\");\n");
        code.append("            System.out.println(\"EXPECTED_OUTPUT : ").append(expectedOutputFormatted).append("\");\n");
        code.append("            System.out.println(\"ERROR : \" + e.getMessage());\n");
        code.append("            System.out.println(\"EXECUTION_TIME : 0\");\n");
        code.append("            System.out.println(\"TEST_CASE_END\");\n");
        code.append("        }\n\n");

        return code.toString();
    }

    /**
     * Format expected output from database to match OutputHelper.format() output
     * This ensures consistent comparison between expected and actual outputs
     */
    private String formatExpectedOutput(Object expectedOutput) {
        if (expectedOutput == null) return "null";

        // If it's already a string representation, return as-is
        if (expectedOutput instanceof String) {
            return escapeString((String) expectedOutput);
        }

        // If it's a number, boolean, or other primitive wrapper
        if (expectedOutput instanceof Number || expectedOutput instanceof Boolean) {
            return String.valueOf(expectedOutput);
        }

        // If it's a List, format it consistently
        if (expectedOutput instanceof List) {
            return escapeString(formatList((List<?>) expectedOutput));
        }

        return escapeString(String.valueOf(expectedOutput));
    }

    /**
     * Format a list to match OutputHelper.format() output
     */
    private String formatList(List<?> list) {
        if (list == null) return "null";
        if (list.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof List) {
                sb.append(formatList((List<?>) item));
            } else {
                sb.append(item);
            }
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Generate deep copy expression based on type
     * Keep it simple - only deep copy known primitive array/list types
     */
    private String deepCopyExpression(String valueExpr, String type) {
        // CRITICAL: Never try to deep copy null
        if (valueExpr.equals("null")) {
            return "null";
        }

        // Only deep copy known primitive types - for everything else, pass as-is
        switch (type) {
            case "int[]":
            case "long[]":
            case "double[]":
            case "float[]":
            case "char[]":
            case "boolean[]":
            case "String[]":
                return valueExpr + ".clone()";

            case "int[][]":
                return "Arrays.stream(" + valueExpr + ").map(int[]::clone).toArray(int[][]::new)";
            case "char[][]":
                return "Arrays.stream(" + valueExpr + ").map(char[]::clone).toArray(char[][]::new)";
            case "String[][]":
                return "Arrays.stream(" + valueExpr + ").map(String[]::clone).toArray(String[][]::new)";

            case "List<Integer>":
            case "List<Long>":
            case "List<String>":
            case "List<Double>":
            case "List<Boolean>":
                return "new ArrayList<>(" + valueExpr + ")";

            case "List<List<Integer>>":
            case "List<List<Long>>":
            case "List<List<String>>":
                return valueExpr + ".stream().map(ArrayList::new).collect(Collectors.toList())";

            default:
                // For any unknown/custom types, just pass as-is (no deep copy)
                return valueExpr;
        }
    }

    /**
     * Convert Java object value to Java code literal
     */
    private String convertToJavaCode(Object value, String type) {
        if (value == null) return "null";

        // Handle 2D arrays
        if (type.equals("int[][]")) {
            return "new int[][] " + convert2DIntArray((List<?>) value);
        }
        if (type.equals("char[][]")) {
            return "new char[][] " + convert2DCharArray((List<?>) value);
        }
        if (type.equals("String[][]")) {
            return "new String[][] " + convert2DStringArray((List<?>) value);
        }

        // Handle 1D arrays
        if (type.equals("int[]")) {
            return "new int[] " + convert1DIntArray((List<?>) value);
        }
        if (type.equals("long[]")) {
            return "new long[] " + convert1DLongArray((List<?>) value);
        }
        if (type.equals("double[]")) {
            return "new double[] " + convert1DDoubleArray((List<?>) value);
        }
        if (type.equals("float[]")) {
            return "new float[] " + convert1DFloatArray((List<?>) value);
        }
        if (type.equals("String[]")) {
            return "new String[] " + convertStringArray((List<?>) value);
        }
        if (type.equals("char[]")) {
            return convert1DCharArray(value);
        }
        if (type.equals("boolean[]")) {
            return "new boolean[] " + convert1DBooleanArray((List<?>) value);
        }

        // Handle List types
        if (type.equals("List<Integer>")) {
            return convertToIntegerList((List<?>) value);
        }
        if (type.equals("List<Long>")) {
            return convertToLongList((List<?>) value);
        }
        if (type.equals("List<String>")) {
            return convertToStringList((List<?>) value);
        }
        if (type.equals("List<List<Integer>>")) {
            return convertTo2DIntegerList((List<?>) value);
        }
        if (type.equals("List<List<String>>")) {
            return convertTo2DStringList((List<?>) value);
        }
        if (type.startsWith("List<")) {
            // Generic list handling
            return convertToGenericList((List<?>) value);
        }

        // Handle primitives
        if (type.equals("int") || type.equals("long") || type.equals("double") || type.equals("float")) {
            return String.valueOf(value);
        }
        if (type.equals("boolean")) {
            return String.valueOf(value);
        }
        if (type.equals("char")) {
            return "'" + escapeChar(String.valueOf(value)) + "'";
        }
        if (type.equals("String")) {
            return "\"" + escapeString(String.valueOf(value)) + "\"";
        }

        return String.valueOf(value);
    }

    // ==================== Array Conversion Methods ====================

    private String convert2DIntArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            List<?> row = (List<?>) array.get(i);
            sb.append("{");
            for (int j = 0; j < row.size(); j++) {
                sb.append(row.get(j));
                if (j < row.size() - 1) sb.append(", ");
            }
            sb.append("}");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert2DCharArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            Object row = array.get(i);
            sb.append("{");
            if (row instanceof String) {
                String str = (String) row;
                for (int j = 0; j < str.length(); j++) {
                    sb.append("'").append(escapeChar(String.valueOf(str.charAt(j)))).append("'");
                    if (j < str.length() - 1) sb.append(", ");
                }
            } else if (row instanceof List) {
                List<?> chars = (List<?>) row;
                for (int j = 0; j < chars.size(); j++) {
                    sb.append("'").append(escapeChar(String.valueOf(chars.get(j)))).append("'");
                    if (j < chars.size() - 1) sb.append(", ");
                }
            }
            sb.append("}");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert2DStringArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            List<?> row = (List<?>) array.get(i);
            sb.append("{");
            for (int j = 0; j < row.size(); j++) {
                sb.append("\"").append(escapeString(String.valueOf(row.get(j)))).append("\"");
                if (j < row.size() - 1) sb.append(", ");
            }
            sb.append("}");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DIntArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DLongArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i)).append("L");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DDoubleArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DFloatArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i)).append("f");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DBooleanArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append(array.get(i));
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    private String convert1DCharArray(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.isEmpty()) return "new char[] {}";
            StringBuilder sb = new StringBuilder("new char[] {");
            for (int i = 0; i < str.length(); i++) {
                sb.append("'").append(escapeChar(String.valueOf(str.charAt(i)))).append("'");
                if (i < str.length() - 1) sb.append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof List) {
            List<?> array = (List<?>) value;
            if (array.isEmpty()) return "new char[] {}";
            StringBuilder sb = new StringBuilder("new char[] {");
            for (int i = 0; i < array.size(); i++) {
                sb.append("'").append(escapeChar(String.valueOf(array.get(i)))).append("'");
                if (i < array.size() - 1) sb.append(", ");
            }
            sb.append("}");
            return sb.toString();
        }
        return "new char[] {}";
    }

    private String convertStringArray(List<?> array) {
        if (array == null || array.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.size(); i++) {
            sb.append("\"").append(escapeString(String.valueOf(array.get(i)))).append("\"");
            if (i < array.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    // ==================== List Conversion Methods ====================

    private String convertToIntegerList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";
        StringBuilder sb = new StringBuilder("new ArrayList<>(Arrays.asList(");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("))");
        return sb.toString();
    }

    private String convertToLongList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";
        StringBuilder sb = new StringBuilder("new ArrayList<>(Arrays.asList(");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append("L");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("))");
        return sb.toString();
    }

    private String convertToStringList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";
        StringBuilder sb = new StringBuilder("new ArrayList<>(Arrays.asList(");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeString(String.valueOf(list.get(i)))).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("))");
        return sb.toString();
    }

    private String convertTo2DIntegerList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";
        StringBuilder sb = new StringBuilder("new ArrayList<>(Arrays.asList(");
        for (int i = 0; i < list.size(); i++) {
            List<?> inner = (List<?>) list.get(i);
            sb.append("new ArrayList<>(Arrays.asList(");
            for (int j = 0; j < inner.size(); j++) {
                sb.append(inner.get(j));
                if (j < inner.size() - 1) sb.append(", ");
            }
            sb.append("))");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("))");
        return sb.toString();
    }

    private String convertTo2DStringList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";
        StringBuilder sb = new StringBuilder("new ArrayList<>(Arrays.asList(");
        for (int i = 0; i < list.size(); i++) {
            List<?> inner = (List<?>) list.get(i);
            sb.append("new ArrayList<>(Arrays.asList(");
            for (int j = 0; j < inner.size(); j++) {
                sb.append("\"").append(escapeString(String.valueOf(inner.get(j)))).append("\"");
                if (j < inner.size() - 1) sb.append(", ");
            }
            sb.append("))");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("))");
        return sb.toString();
    }

    private String convertToGenericList(List<?> list) {
        if (list == null || list.isEmpty()) return "new ArrayList<>()";

        // Check if it's a nested list
        if (!list.isEmpty() && list.get(0) instanceof List) {
            return convertTo2DIntegerList(list);
        }

        // Check the type of elements
        Object first = list.get(0);
        if (first instanceof String) {
            return convertToStringList(list);
        }
        return convertToIntegerList(list);
    }

    // ==================== String Escape Methods ====================

    private String escapeString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeChar(String str) {
        if (str == null || str.isEmpty()) return "";
        char c = str.charAt(0);
        switch (c) {
            case '\\': return "\\\\";
            case '\'': return "\\'";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            default: return String.valueOf(c);
        }
    }

    // ==================== Inner Classes ====================

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