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

    public String generateFromTemplate(
            String adminTemplate,
            String userCode,
            List<RunTestCaseInput> testCases) {

        logger.info("Generating C++ code from template...");
        logger.info("Number of test cases: {}", testCases.size());

        String testCaseTemplate = extractBetween(
                adminTemplate,
                "/*TEST_CASE_TEMPLATE_START*/",
                "/*TEST_CASE_TEMPLATE_END*/"
        );

        logger.info("Extracted test case template (length: {} chars)", testCaseTemplate.length());

        StringBuilder allTestCases = new StringBuilder();

        for (int i = 0; i < testCases.size(); i++) {
            logger.info("Processing test case {}: input = {}", i + 1, testCases.get(i).getInput());

            String filledBlock = testCaseTemplate;
            List<Object> inputs = testCases.get(i).getInput();

            for (int j = 0; j < inputs.size(); j++) {
                String placeholder = "{{INPUT_" + j + "}}";
                String cppLiteral = convertToCppLiteral(inputs.get(j));

                logger.info("Replacing {} with: {}", placeholder, cppLiteral);

                filledBlock = filledBlock.replace(placeholder, cppLiteral);
            }

            allTestCases.append(filledBlock).append("\n");
        }

        String finalCode = adminTemplate
                .replace(
                        "/*TEST_CASE_TEMPLATE_START*/" + testCaseTemplate + "/*TEST_CASE_TEMPLATE_END*/",
                        allTestCases.toString()
                )
                .replace("/*USER_CODE_PLACEHOLDER*/", userCode);

        logger.info("Final C++ code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }

    private String convertToCppLiteral(Object value) {
        if (value == null) {
            return "nullptr";
        }

        if (value instanceof Long) {
            return value + "L";
        }

        if (value instanceof Float) {
            return value + "f";
        }

        if (value instanceof Number) {
            return String.valueOf(value);
        }

        if (value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof String) {
            String str = (String) value;
            if (str.length() == 1) {
                return "'" + escapeChar(str.charAt(0)) + "'";
            }
            return "\"" + escapeString(str) + "\"";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;

            if (list.isEmpty()) {
                return "{}";
            }

            int depth = getListDepth(list);

            if (depth == 1) {
                return "{" + convertListToString(list) + "}";
            } else if (depth == 2) {
                return "{" + list.stream()
                        .map(inner -> "{" + convertListToString((List<?>) inner) + "}")
                        .collect(Collectors.joining(", ")) + "}";
            } else {
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
                        String str = (String) item;
                        if (str.length() == 1) {
                            return "'" + escapeChar(str.charAt(0)) + "'";
                        }
                        return "\"" + escapeString(str) + "\"";
                    }
                    if (item instanceof Long) return item + "L";
                    if (item instanceof Float) return item + "f";
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

    private String escapeChar(char c) {
        switch (c) {
            case '\\': return "\\\\";
            case '\'': return "\\'";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            default: return String.valueOf(c);
        }
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