// src/main/java/com/algoarena/service/compiler/CodeMergerService.java
package com.algoarena.service.compiler;

import com.algoarena.model.Question;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CodeMergerService {

    /**
     * Merge user code with template for BATCH execution (all test cases in one run)
     */
    public String mergeBatchCode(String userCode, String template, String language, 
                                  List<Question.Testcase> testcases) {
        if ("java".equalsIgnoreCase(language)) {
            return mergeJavaBatchCode(userCode, testcases);
        } else if ("cpp".equalsIgnoreCase(language) || "c++".equalsIgnoreCase(language)) {
            return mergeCppBatchCode(userCode, testcases);
        } else if ("python".equalsIgnoreCase(language)) {
            return mergePythonBatchCode(userCode, testcases);
        } else if ("javascript".equalsIgnoreCase(language) || "js".equalsIgnoreCase(language)) {
            return mergeJavaScriptBatchCode(userCode, testcases);
        }
        
        return template.replace("// Your implementation here", userCode);
    }

    // ==================== JAVA BATCH ====================
    private String mergeJavaBatchCode(String userCode, List<Question.Testcase> testcases) {
        StringBuilder code = new StringBuilder();
        
        code.append("import java.util.*;\n\n");
        code.append("public class Main {\n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        Solution solution = new Solution();\n\n");
        
        // Add each test case
        for (int i = 0; i < testcases.size(); i++) {
            Question.Testcase tc = testcases.get(i);
            
            code.append("        // Test Case ").append(tc.getId()).append("\n");
            code.append("        System.out.println(\"TC_START:").append(tc.getId()).append("\");\n");
            code.append("        long startTime").append(i).append(" = System.currentTimeMillis();\n");
            code.append("        try {\n");
            
            // Generate input
            code.append(generateJavaInput(tc.getInput(), i));
            
            // Call solution
            code.append("            List<List<Integer>> result").append(i)
                .append(" = solution.threeSum(nums").append(i).append(");\n");
            
            // Sort result for consistent output
            code.append("            result").append(i).append(".sort((a, b) -> {\n");
            code.append("                for (int j = 0; j < 3; j++) {\n");
            code.append("                    if (!a.get(j).equals(b.get(j))) return a.get(j) - b.get(j);\n");
            code.append("                }\n");
            code.append("                return 0;\n");
            code.append("            });\n");
            
            // Print result
            code.append("            System.out.print(\"OUTPUT:\");\n");
            code.append("            printResult(result").append(i).append(");\n");
            
            code.append("        } catch (Exception e) {\n");
            code.append("            System.out.println(\"OUTPUT:ERROR:\" + e.getMessage());\n");
            code.append("        }\n");
            
            code.append("        long endTime").append(i).append(" = System.currentTimeMillis();\n");
            code.append("        System.out.println(\"TIME:\" + (endTime").append(i)
                .append(" - startTime").append(i).append("));\n");
            code.append("        System.out.println(\"TC_END:").append(tc.getId()).append("\");\n\n");
        }
        
        code.append("    }\n\n");
        
        // Helper method to print result
        code.append("    private static void printResult(List<List<Integer>> result) {\n");
        code.append("        System.out.print(\"[\");\n");
        code.append("        for (int i = 0; i < result.size(); i++) {\n");
        code.append("            System.out.print(\"[\");\n");
        code.append("            List<Integer> triplet = result.get(i);\n");
        code.append("            for (int j = 0; j < triplet.size(); j++) {\n");
        code.append("                System.out.print(triplet.get(j));\n");
        code.append("                if (j < triplet.size() - 1) System.out.print(\",\");\n");
        code.append("            }\n");
        code.append("            System.out.print(\"]\");\n");
        code.append("            if (i < result.size() - 1) System.out.print(\",\");\n");
        code.append("        }\n");
        code.append("        System.out.println(\"]\");\n");
        code.append("    }\n");
        code.append("}\n\n");
        
        // Add user's Solution class (make it package-private)
        String cleanedUserCode = userCode.trim();
        cleanedUserCode = cleanedUserCode.replace("public class Solution", "class Solution");
        code.append(cleanedUserCode);
        
        return code.toString();
    }

    private String generateJavaInput(Map<String, Object> input, int index) {
        StringBuilder code = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Integer> nums = (List<Integer>) input.get("nums");
        
        code.append("            int[] nums").append(index).append(" = {");
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) code.append(", ");
            code.append(nums.get(i));
        }
        code.append("};\n");
        
        return code.toString();
    }

    // ==================== C++ BATCH ====================
    private String mergeCppBatchCode(String userCode, List<Question.Testcase> testcases) {
        StringBuilder code = new StringBuilder();
        
        code.append("#include <iostream>\n");
        code.append("#include <vector>\n");
        code.append("#include <algorithm>\n");
        code.append("#include <chrono>\n");
        code.append("using namespace std;\n");
        code.append("using namespace std::chrono;\n\n");
        
        // Add user's Solution class
        code.append(userCode.trim()).append("\n\n");
        
        // Helper function to print result
        code.append("void printResult(vector<vector<int>>& result) {\n");
        code.append("    cout << \"[\";\n");
        code.append("    for (int i = 0; i < result.size(); i++) {\n");
        code.append("        cout << \"[\";\n");
        code.append("        for (int j = 0; j < result[i].size(); j++) {\n");
        code.append("            cout << result[i][j];\n");
        code.append("            if (j < result[i].size() - 1) cout << \",\";\n");
        code.append("        }\n");
        code.append("        cout << \"]\";\n");
        code.append("        if (i < result.size() - 1) cout << \",\";\n");
        code.append("    }\n");
        code.append("    cout << \"]\" << endl;\n");
        code.append("}\n\n");
        
        code.append("int main() {\n");
        code.append("    Solution solution;\n\n");
        
        // Add each test case
        for (int i = 0; i < testcases.size(); i++) {
            Question.Testcase tc = testcases.get(i);
            
            code.append("    // Test Case ").append(tc.getId()).append("\n");
            code.append("    cout << \"TC_START:").append(tc.getId()).append("\" << endl;\n");
            code.append("    auto start").append(i).append(" = high_resolution_clock::now();\n");
            
            // Generate input
            code.append(generateCppInput(tc.getInput(), i));
            
            // Call solution
            code.append("    vector<vector<int>> result").append(i)
                .append(" = solution.threeSum(nums").append(i).append(");\n");
            
            // Sort result
            code.append("    sort(result").append(i).append(".begin(), result").append(i).append(".end());\n");
            
            // Print result
            code.append("    cout << \"OUTPUT:\";\n");
            code.append("    printResult(result").append(i).append(");\n");
            
            code.append("    auto end").append(i).append(" = high_resolution_clock::now();\n");
            code.append("    auto duration").append(i).append(" = duration_cast<milliseconds>(end")
                .append(i).append(" - start").append(i).append(");\n");
            code.append("    cout << \"TIME:\" << duration").append(i).append(".count() << endl;\n");
            code.append("    cout << \"TC_END:").append(tc.getId()).append("\" << endl;\n\n");
        }
        
        code.append("    return 0;\n");
        code.append("}\n");
        
        return code.toString();
    }

    private String generateCppInput(Map<String, Object> input, int index) {
        StringBuilder code = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Integer> nums = (List<Integer>) input.get("nums");
        
        code.append("    vector<int> nums").append(index).append(" = {");
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) code.append(", ");
            code.append(nums.get(i));
        }
        code.append("};\n");
        
        return code.toString();
    }

    // ==================== PYTHON BATCH ====================
    private String mergePythonBatchCode(String userCode, List<Question.Testcase> testcases) {
        StringBuilder code = new StringBuilder();
        
        code.append("import time\n");
        code.append("import json\n\n");
        
        // Add user's Solution class
        code.append(userCode.trim()).append("\n\n");
        
        code.append("if __name__ == \"__main__\":\n");
        code.append("    solution = Solution()\n\n");
        
        // Add each test case
        for (int i = 0; i < testcases.size(); i++) {
            Question.Testcase tc = testcases.get(i);
            
            code.append("    # Test Case ").append(tc.getId()).append("\n");
            code.append("    print(\"TC_START:").append(tc.getId()).append("\")\n");
            code.append("    start_time").append(i).append(" = time.time()\n");
            code.append("    try:\n");
            
            // Generate input
            code.append(generatePythonInput(tc.getInput(), i));
            
            // Call solution
            code.append("        result").append(i)
                .append(" = solution.threeSum(nums").append(i).append(")\n");
            
            // Sort result
            code.append("        result").append(i).append(".sort()\n");
            
            // Print result
            code.append("        print(\"OUTPUT:\" + json.dumps(result").append(i).append("))\n");
            
            code.append("    except Exception as e:\n");
            code.append("        print(f\"OUTPUT:ERROR:{str(e)}\")\n");
            
            code.append("    end_time").append(i).append(" = time.time()\n");
            code.append("    print(f\"TIME:{int((end_time").append(i)
                .append(" - start_time").append(i).append(") * 1000)}\")\n");
            code.append("    print(\"TC_END:").append(tc.getId()).append("\")\n\n");
        }
        
        return code.toString();
    }

    private String generatePythonInput(Map<String, Object> input, int index) {
        StringBuilder code = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Integer> nums = (List<Integer>) input.get("nums");
        
        code.append("        nums").append(index).append(" = [");
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) code.append(", ");
            code.append(nums.get(i));
        }
        code.append("]\n");
        
        return code.toString();
    }

    // ==================== JAVASCRIPT BATCH ====================
    private String mergeJavaScriptBatchCode(String userCode, List<Question.Testcase> testcases) {
        StringBuilder code = new StringBuilder();
        
        // Add user's Solution class
        code.append(userCode.trim()).append("\n\n");
        
        code.append("(function() {\n");
        code.append("    const solution = new Solution();\n\n");
        
        // Add each test case
        for (int i = 0; i < testcases.size(); i++) {
            Question.Testcase tc = testcases.get(i);
            
            code.append("    // Test Case ").append(tc.getId()).append("\n");
            code.append("    console.log(\"TC_START:").append(tc.getId()).append("\");\n");
            code.append("    const startTime").append(i).append(" = Date.now();\n");
            code.append("    try {\n");
            
            // Generate input
            code.append(generateJavaScriptInput(tc.getInput(), i));
            
            // Call solution
            code.append("        const result").append(i)
                .append(" = solution.threeSum(nums").append(i).append(");\n");
            
            // Sort result
            code.append("        result").append(i).append(".sort((a, b) => {\n");
            code.append("            for (let j = 0; j < 3; j++) {\n");
            code.append("                if (a[j] !== b[j]) return a[j] - b[j];\n");
            code.append("            }\n");
            code.append("            return 0;\n");
            code.append("        });\n");
            
            // Print result
            code.append("        console.log(\"OUTPUT:\" + JSON.stringify(result").append(i).append("));\n");
            
            code.append("    } catch (e) {\n");
            code.append("        console.log(\"OUTPUT:ERROR:\" + e.message);\n");
            code.append("    }\n");
            
            code.append("    const endTime").append(i).append(" = Date.now();\n");
            code.append("    console.log(\"TIME:\" + (endTime").append(i)
                .append(" - startTime").append(i).append("));\n");
            code.append("    console.log(\"TC_END:").append(tc.getId()).append("\");\n\n");
        }
        
        code.append("})();\n");
        
        return code.toString();
    }

    private String generateJavaScriptInput(Map<String, Object> input, int index) {
        StringBuilder code = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Integer> nums = (List<Integer>) input.get("nums");
        
        code.append("        const nums").append(index).append(" = [");
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) code.append(", ");
            code.append(nums.get(i));
        }
        code.append("];\n");
        
        return code.toString();
    }

    // ==================== LEGACY: Single test case merge (kept for backward compatibility) ====================
    public String mergeCode(String userCode, String template, String language) {
        if ("java".equalsIgnoreCase(language)) {
            return mergeJavaCode(userCode, template);
        } else if ("cpp".equalsIgnoreCase(language) || "c++".equalsIgnoreCase(language)) {
            return mergeCppCode(userCode, template);
        }
        
        return template.replace("// Your implementation here", userCode);
    }

    private String mergeJavaCode(String userCode, String template) {
        int solutionStart = template.indexOf("class Solution {");
        if (solutionStart == -1) return template;
        
        int solutionEnd = findMatchingBrace(template, solutionStart);
        if (solutionEnd == -1) return template;
        
        String before = template.substring(0, solutionStart);
        String after = template.substring(solutionEnd);
        
        String mergedCode = before + userCode.trim() + "\n\n" + after;
        mergedCode = mergedCode.replace("public class Solution", "class Solution");
        
        return mergedCode;
    }

    private String mergeCppCode(String userCode, String template) {
        int solutionStart = template.indexOf("class Solution {");
        if (solutionStart == -1) return template;
        
        int solutionEnd = findMatchingBrace(template, solutionStart);
        if (solutionEnd == -1) return template;
        
        String before = template.substring(0, solutionStart);
        String after = template.substring(solutionEnd);
        
        return before + userCode.trim() + "\n\n" + after;
    }

    private int findMatchingBrace(String code, int start) {
        int braceCount = 0;
        boolean foundFirst = false;
        
        for (int i = start; i < code.length(); i++) {
            if (code.charAt(i) == '{') {
                braceCount++;
                foundFirst = true;
            } else if (code.charAt(i) == '}') {
                braceCount--;
                if (foundFirst && braceCount == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    // ==================== FORMAT INPUT (DEPRECATED - not used in batch mode) ====================
    public String formatInput(Map<String, Object> input, String language) {
        Object numsObj = input.get("nums");
        
        if (numsObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> numsList = (java.util.List<Integer>) numsObj;
            
            StringBuilder sb = new StringBuilder();
            sb.append(numsList.size()).append("\n");
            
            for (int i = 0; i < numsList.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(numsList.get(i));
            }
            
            return sb.toString();
        }
        
        return input.toString();
    }
}