// src/main/java/com/algoarena/service/compiler/runmode/JavaScriptTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JavaScriptTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptTemplateGenerator.class);

    /**
     * Generate executable JavaScript code from admin's template
     */
    public String generateFromTemplate(
            String adminTemplate,
            String userCode,
            List<RunTestCaseInput> testCases) {

        logger.info("Generating JavaScript code from template...");
        logger.info("Number of test cases: {}", testCases.size());

        // 1. Extract test case template block
        String testCaseTemplate = extractBetween(
                adminTemplate,
                "// {{TEST_CASE_TEMPLATE_START}}",
                "// {{TEST_CASE_TEMPLATE_END}}"
        );

        logger.info("Extracted test case template (length: {} chars)", testCaseTemplate.length());

        // 2. Fill test cases
        StringBuilder allTestCases = new StringBuilder();

        for (int i = 0; i < testCases.size(); i++) {
            logger.info("Processing test case {}: input = {}", i + 1, testCases.get(i).getInput());

            String filledBlock = testCaseTemplate;

            // Replace {{INPUT_X}} placeholders
            List<Object> inputs = testCases.get(i).getInput();

            for (int j = 0; j < inputs.size(); j++) {
                String placeholder = "{{INPUT_" + j + "}}";
                String jsLiteral = convertToJsLiteral(inputs.get(j));

                logger.info("Replacing {} with: {}", placeholder, jsLiteral);

                filledBlock = filledBlock.replace(placeholder, jsLiteral);
            }

            allTestCases.append(filledBlock).append("\n");
        }

        // 3. Build final code
        String finalCode = adminTemplate
                .replace(
                        "// {{TEST_CASE_TEMPLATE_START}}" + testCaseTemplate + "// {{TEST_CASE_TEMPLATE_END}}",
                        allTestCases.toString()
                )
                .replace("// {{USER_CODE_PLACEHOLDER}}", userCode);

        logger.info("Final JavaScript code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }

    /**
     * Convert JSON value to JavaScript literal
     */
    private String convertToJsLiteral(Object value) {
        if (value == null) {
            return "null";
        }

        // Boolean
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }

        // Numbers
        if (value instanceof Number) {
            return String.valueOf(value);
        }

        // String
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }

        // List/Array
        if (value instanceof List) {
            List<?> list = (List<?>) value;

            if (list.isEmpty()) {
                return "[]";
            }

            // Check depth
            int depth = getListDepth(list);

            if (depth == 1) {
                // 1D array: [1, 2, 3]
                return "[" + convertListToString(list) + "]";
            } else {
                // Nested arrays: [[1,2], [3,4]]
                return "[" + list.stream()
                        .map(this::convertToJsLiteral)
                        .collect(Collectors.joining(", ")) + "]";
            }
        }

        return String.valueOf(value);
    }

    private int getListDepth(List<?> list) {
        if (list.isEmpty()) return 1;
        Object first = list.get(0);
        if (first instanceof List) {
            return 1 + getListDepth((List<?>) first);
        }
        return 1;
    }

    private String convertListToString(List<?> list) {
        return list.stream()
                .map(item -> {
                    if (item == null) return "null";
                    if (item instanceof String) {
                        return "\"" + escapeString((String) item) + "\"";
                    }
                    return String.valueOf(item);
                })
                .collect(Collectors.joining(", "));
    }

    private String escapeString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractBetween(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        int endIdx = text.indexOf(end);

        if (startIdx == -1 || endIdx == -1) {
            throw new RuntimeException("Invalid template: missing markers " + start + " or " + end);
        }

        return text.substring(startIdx + start.length(), endIdx);
    }
}