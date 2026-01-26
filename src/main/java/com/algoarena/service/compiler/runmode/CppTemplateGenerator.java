// src/main/java/com/algoarena/service/compiler/runmode/CppTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CppTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CppTemplateGenerator.class);

    /**
     * Generate executable C++ code from admin's template
     */
    public String generateFromTemplate(
            String adminTemplate,
            String userCode,
            List<RunTestCaseInput> testCases) {

        logger.info("Generating C++ code from template...");
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
                String cppLiteral = convertToCppLiteral(inputs.get(j));

                logger.info("Replacing {} with: {}", placeholder, cppLiteral);

                filledBlock = filledBlock.replace(placeholder, cppLiteral);
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

        logger.info("Final C++ code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }

    /**
     * Convert JSON value to C++ literal
     */
    private String convertToCppLiteral(Object value) {
        if (value == null) {
            return "nullptr";
        }

        // Boolean (C++ uses true/false)
        if (value instanceof Boolean) {
            return String.valueOf(value).toLowerCase();
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
                return "{}";
            }

            // Check depth
            int depth = getListDepth(list);

            if (depth == 1) {
                // 1D vector: {1, 2, 3}
                return "{" + convertListToString(list) + "}";
            } else {
                // Nested vectors: {{1,2}, {3,4}}
                return "{" + list.stream()
                        .map(this::convertToCppLiteral)
                        .collect(Collectors.joining(", ")) + "}";
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
                    if (item == null) return "nullptr";
                    if (item instanceof String) {
                        return "\"" + escapeString((String) item) + "\"";
                    }
                    if (item instanceof Boolean) {
                        return String.valueOf(item).toLowerCase();
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