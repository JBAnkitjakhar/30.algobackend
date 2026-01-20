// src/main/java/com/algoarena/service/compiler/QuestionCompilerService.java
package com.algoarena.service.compiler;

import com.algoarena.dto.compiler.*;
import com.algoarena.dto.dsa.ApproachDetailDTO;
import com.algoarena.model.Question;
import com.algoarena.model.User;
import com.algoarena.model.UserApproaches;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.service.dsa.ApproachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class QuestionCompilerService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionCompilerService.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PistonService pistonService;

    @Autowired
    private CodeMergerService codeMergerService;

    @Autowired
    private OutputComparatorService outputComparatorService;

    @Autowired
    private ApproachService approachService;

    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private static final int MAX_CONCURRENT = 2;

    /**
     * Run code with selected testcases (1-5)
     */
    public CodeExecutionResult runCode(String questionId, RunCodeRequest request, String userId) {
        // Check container availability
        if (!tryAcquireSlot()) {
            throw new IllegalStateException("Execution container is busy. Please try again shortly.");
        }

        try {
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

            // Validate language has template
            if (!question.getGeneralTemplate().containsKey(request.getLanguage())) {
                CodeExecutionResult result = new CodeExecutionResult();
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError("Language not supported for this question");
                return result;
            }

            // Get selected testcases
            List<Question.Testcase> selectedTestcases = request.getTestCaseIds().stream()
                .map(id -> question.getTestcases().stream()
                    .filter(tc -> tc.getId().equals(id))
                    .findFirst()
                    .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (selectedTestcases.isEmpty()) {
                CodeExecutionResult result = new CodeExecutionResult();
                result.setSuccess(false);
                result.setError("No valid test cases found");
                return result;
            }

            // Execute in BATCH mode
            return executeBatch(
                question, 
                request.getCode(), 
                request.getLanguage(), 
                selectedTestcases,
                false // isSubmit = false
            );

        } finally {
            releaseSlot();
        }
    }

    /**
     * Submit code - tests ALL testcases and saves approach
     */
    public CodeExecutionResult submitCode(String questionId, SubmitCodeRequest request, User currentUser) {
        // Check container availability
        if (!tryAcquireSlot()) {
            throw new IllegalStateException("Execution container is busy. Please try again shortly.");
        }

        try {
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

            // Validate language
            if (!question.getGeneralTemplate().containsKey(request.getLanguage())) {
                CodeExecutionResult result = new CodeExecutionResult();
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError("Language not supported for this question");
                
                // Save compilation error approach
                saveApproach(question, request, result, currentUser);
                return result;
            }

            // Use ALL testcases
            List<Question.Testcase> allTestcases = question.getTestcases();

            // Execute in BATCH mode
            CodeExecutionResult result = executeBatch(
                question, 
                request.getCode(), 
                request.getLanguage(), 
                allTestcases,
                true // isSubmit = true
            );

            // Save approach
            saveApproach(question, request, result, currentUser);

            return result;

        } finally {
            releaseSlot();
        }
    }

    /**
     * BATCH EXECUTION: All test cases run in a single Piston call
     */
    private CodeExecutionResult executeBatch(
            Question question,
            String userCode,
            String language,
            List<Question.Testcase> testcases,
            boolean isSubmit) {

        CodeExecutionResult result = new CodeExecutionResult();

        try {
            logger.info("========== STARTING BATCH EXECUTION ==========");
            logger.info("Language: {}", language);
            logger.info("Number of test cases: {}", testcases.size());
            logger.info("User code length: {} chars", userCode.length());
            logger.info("Is submit: {}", isSubmit);

            // 1️⃣ Merge code with BATCH template
            logger.info("Step 1: Merging code with batch template...");
            String template = question.getGeneralTemplate().get(language);
            String mergedCode = codeMergerService.mergeBatchCode(
                userCode, 
                template, 
                language, 
                testcases
            );

            logger.info("========== MERGED BATCH CODE START ==========");
            logger.info("{}", mergedCode);
            logger.info("========== MERGED BATCH CODE END ==========");

            // 2️⃣ Execute ONCE with all test cases embedded
            logger.info("Step 2: Executing batch code (single Piston call)...");
            ExecutionRequest execRequest = new ExecutionRequest(
                language,
                pistonService.getLanguageVersion(language),
                mergedCode
            );
            // No stdin needed - inputs are hardcoded in the merged code

            ExecutionResponse execResponse = pistonService.executeCode(execRequest);

            // Check compilation errors
            if (execResponse.hasCompileError()) {
                logger.error("Compilation failed!");
                logger.error("Compile error: {}", execResponse.getCompile().getStderr());
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError(execResponse.getCompile().getStderr());
                result.setFailedTestCaseIndex(testcases.get(0).getId());
                return result;
            }

            // Check runtime errors
            if (execResponse.hasRuntimeError()) {
                logger.error("Runtime error detected!");
                logger.error("Runtime error: {}", execResponse.getRun().getStderr());
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError("Runtime Error: " + execResponse.getRun().getStderr());
                result.setFailedTestCaseIndex(testcases.get(0).getId());
                return result;
            }

            logger.info("Code compiled and executed successfully!");

            // 3️⃣ Parse batch output
            String stdout = execResponse.getRun().getStdout();
            logger.info("========== BATCH OUTPUT START ==========");
            logger.info("{}", stdout);
            logger.info("========== BATCH OUTPUT END ==========");

            List<TestCaseResult> testCaseResults = parseBatchOutput(stdout);

            if (testCaseResults.isEmpty()) {
                logger.error("Failed to parse any test case results from output!");
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError("Failed to parse execution output");
                return result;
            }

            // 4️⃣ Analyze results
            logger.info("Step 3: Analyzing {} test case results...", testCaseResults.size());
            long maxTime = 0;
            int passedCount = 0;
            Integer firstFailedIndex = null;
            String finalVerdict = "ACCEPTED";

            for (TestCaseResult tcResult : testCaseResults) {
                Question.Testcase testcase = testcases.stream()
                    .filter(tc -> tc.getId().equals(tcResult.getIndex()))
                    .findFirst()
                    .orElse(null);

                if (testcase == null) {
                    logger.warn("Test case {} not found in question testcases", tcResult.getIndex());
                    continue;
                }

                logger.info("--- Analyzing test case {} ---", tcResult.getIndex());
                logger.info("Output: {}", tcResult.getOutput());
                logger.info("Time: {}ms", tcResult.getTimeMs());
                logger.info("Expected: {}", testcase.getExpectedOutput());

                // Track max time
                maxTime = Math.max(maxTime, tcResult.getTimeMs() != null ? tcResult.getTimeMs() : 0);

                // Check for errors in output
                if (tcResult.getOutput() != null && tcResult.getOutput().startsWith("ERROR:")) {
                    logger.error("Test case {} had runtime error: {}", tcResult.getIndex(), tcResult.getOutput());
                    tcResult.setStatus("error");
                    if (firstFailedIndex == null) {
                        firstFailedIndex = testcase.getId();
                        finalVerdict = "WRONG_ANSWER";
                    }
                    continue;
                }

                // Compare output
                boolean outputMatch = outputComparatorService.compare(
                    tcResult.getOutput(),
                    testcase.getExpectedOutput()
                );

                logger.info("Output comparison: {}", outputMatch ? "MATCH" : "MISMATCH");

                // Check TLE
                boolean isTLE = tcResult.getTimeMs() != null && 
                               tcResult.getTimeMs() > testcase.getExpectedTimeLimit();

                if (!outputMatch) {
                    logger.warn("Output mismatch for test case {}", tcResult.getIndex());
                    tcResult.setStatus("wrong");
                    if (firstFailedIndex == null) {
                        firstFailedIndex = testcase.getId();
                        finalVerdict = "WRONG_ANSWER";
                    }
                } else if (isTLE) {
                    logger.warn("TLE for test case {}: {}ms > {}ms", 
                        tcResult.getIndex(), tcResult.getTimeMs(), testcase.getExpectedTimeLimit());
                    tcResult.setStatus("tle");
                    if (firstFailedIndex == null) {
                        firstFailedIndex = testcase.getId();
                        finalVerdict = "TLE";
                    }
                } else {
                    logger.info("Test case {} passed!", tcResult.getIndex());
                    tcResult.setStatus("success");
                    passedCount++;
                }

                // Early exit on submit mode if failed
                if (isSubmit && firstFailedIndex != null) {
                    logger.info("Early exit on submit - first failure at test case {}", firstFailedIndex);
                    break;
                }
            }

            // 5️⃣ Build result
            CodeExecutionResult.ExecutionMetrics metrics = new CodeExecutionResult.ExecutionMetrics();
            metrics.setMaxTimeMs(maxTime);
            metrics.setTotalTestCases(testcases.size());
            metrics.setPassedTestCases(passedCount);
            metrics.setTotalMemoryMb(execResponse.getRun().getMemory() != null ? 
                execResponse.getRun().getMemory() / 1024.0 / 1024.0 : 50.0);

            result.setSuccess(true);
            result.setVerdict(finalVerdict);
            result.setMessage(finalVerdict.equals("ACCEPTED") ? 
                "All test cases passed!" : 
                "Test case " + firstFailedIndex + " failed");
            result.setTestCaseResults(testCaseResults);
            result.setMetrics(metrics);
            result.setFailedTestCaseIndex(firstFailedIndex);

            logger.info("========== BATCH EXECUTION COMPLETE ==========");
            logger.info("Final verdict: {}", finalVerdict);
            logger.info("Passed: {}/{}", passedCount, testcases.size());
            logger.info("Max time: {}ms", maxTime);

            return result;

        } catch (Exception e) {
            logger.error("Exception during batch execution!", e);
            result.setSuccess(false);
            result.setVerdict("WRONG_ANSWER");
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * Parse batch output format:
     * TC_START:1
     * OUTPUT:[[-1,-1,2],[-1,0,1]]
     * TIME:45
     * TC_END:1
     */
    private List<TestCaseResult> parseBatchOutput(String stdout) {
        List<TestCaseResult> results = new ArrayList<>();
        
        if (stdout == null || stdout.trim().isEmpty()) {
            logger.error("Stdout is empty!");
            return results;
        }
        
        String[] lines = stdout.split("\n");
        TestCaseResult currentResult = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("TC_START:")) {
                int id = Integer.parseInt(line.substring("TC_START:".length()).trim());
                currentResult = new TestCaseResult();
                currentResult.setIndex(id);
                logger.debug("Started parsing test case {}", id);
                
            } else if (line.startsWith("OUTPUT:")) {
                if (currentResult != null) {
                    String output = line.substring("OUTPUT:".length()).trim();
                    currentResult.setOutput(output);
                    logger.debug("Parsed output: {}", output);
                }
                
            } else if (line.startsWith("TIME:")) {
                if (currentResult != null) {
                    try {
                        long timeMs = Long.parseLong(line.substring("TIME:".length()).trim());
                        currentResult.setTimeMs(timeMs);
                        logger.debug("Parsed time: {}ms", timeMs);
                    } catch (NumberFormatException e) {
                        logger.error("Failed to parse time: {}", line);
                        currentResult.setTimeMs(0L);
                    }
                }
                
            } else if (line.startsWith("TC_END:")) {
                if (currentResult != null) {
                    results.add(currentResult);
                    logger.debug("Completed parsing test case {}", currentResult.getIndex());
                    currentResult = null;
                }
            }
        }
        
        // If parsing was incomplete, add partial result
        if (currentResult != null) {
            logger.warn("Incomplete test case result detected, adding partial result");
            currentResult.setStatus("incomplete");
            results.add(currentResult);
        }
        
        logger.info("Parsed {} test case results from batch output", results.size());
        return results;
    }

    private void saveApproach(Question question, SubmitCodeRequest request, 
                               CodeExecutionResult result, User currentUser) {
        // Build approach DTO
        ApproachDetailDTO dto = new ApproachDetailDTO();
        dto.setTextContent("Click edit to add your approach description...");
        dto.setCodeContent(request.getCode());
        dto.setCodeLanguage(request.getLanguage());

        // Map verdict to status
        UserApproaches.ApproachStatus status;
        switch (result.getVerdict()) {
            case "ACCEPTED":
                status = UserApproaches.ApproachStatus.ACCEPTED;
                dto.setRuntime(result.getMetrics().getMaxTimeMs());
                dto.setMemory((long) (result.getMetrics().getTotalMemoryMb() * 1024 * 1024));
                break;
            case "TLE":
                status = UserApproaches.ApproachStatus.TIME_LIMIT_EXCEEDED;
                dto.setRuntime(result.getMetrics().getMaxTimeMs());
                dto.setMemory((long) (result.getMetrics().getTotalMemoryMb() * 1024 * 1024));
                
                // Find TLE testcase
                if (result.getFailedTestCaseIndex() != null) {
                    Question.Testcase tleTestcase = question.getTestcases().stream()
                        .filter(tc -> tc.getId().equals(result.getFailedTestCaseIndex()))
                        .findFirst()
                        .orElse(null);
                    
                    if (tleTestcase != null) {
                        ApproachDetailDTO.TestcaseFailureDTO failure = new ApproachDetailDTO.TestcaseFailureDTO();
                        failure.setInput(formatTestcaseInput(tleTestcase.getInput()));
                        failure.setUserOutput("TLE");
                        failure.setExpectedOutput(tleTestcase.getExpectedOutput().toString());
                        dto.setTleTestcase(failure);
                    }
                }
                break;
            default: // WRONG_ANSWER (includes compilation errors)
                status = UserApproaches.ApproachStatus.WRONG_ANSWER;
                
                // Find wrong testcase
                if (result.getFailedTestCaseIndex() != null) {
                    Question.Testcase wrongTestcase = question.getTestcases().stream()
                        .filter(tc -> tc.getId().equals(result.getFailedTestCaseIndex()))
                        .findFirst()
                        .orElse(null);
                    
                    if (wrongTestcase != null) {
                        ApproachDetailDTO.TestcaseFailureDTO failure = new ApproachDetailDTO.TestcaseFailureDTO();
                        failure.setInput(formatTestcaseInput(wrongTestcase.getInput()));
                        
                        TestCaseResult tcResult = result.getTestCaseResults() != null ? 
                            result.getTestCaseResults().stream()
                                .filter(tr -> tr.getIndex() == result.getFailedTestCaseIndex())
                                .findFirst()
                                .orElse(null) : null;
                        
                        failure.setUserOutput(tcResult != null ? tcResult.getOutput() : result.getError());
                        failure.setExpectedOutput(wrongTestcase.getExpectedOutput().toString());
                        dto.setWrongTestcase(failure);
                    }
                }
                break;
        }

        dto.setStatus(status);

        // Save via ApproachService
        approachService.createApproach(currentUser.getId(), question.getId(), dto, currentUser);
    }

    private String formatTestcaseInput(Map<String, Object> input) {
        return input.entrySet().stream()
            .map(e -> e.getKey() + " = " + e.getValue())
            .collect(Collectors.joining(", "));
    }

    private boolean tryAcquireSlot() {
        return activeExecutions.incrementAndGet() <= MAX_CONCURRENT;
    }

    private void releaseSlot() {
        activeExecutions.decrementAndGet();
    }
}