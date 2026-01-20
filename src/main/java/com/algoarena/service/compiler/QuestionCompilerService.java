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

            // Execute and compare
            return executeAndCompare(
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

            // Execute and compare
            CodeExecutionResult result = executeAndCompare(
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

    private CodeExecutionResult executeAndCompare(
            Question question,
            String userCode,
            String language,
            List<Question.Testcase> testcases,
            boolean isSubmit) {

        CodeExecutionResult result = new CodeExecutionResult();

        try {
            logger.info("========== STARTING EXECUTION ==========");
            logger.info("Language: {}", language);
            logger.info("Number of test cases: {}", testcases.size());
            logger.info("User code length: {} chars", userCode.length());

            // 1️⃣ Merge code with general template FIRST
            logger.info("Step 1: Merging code with template...");
            String template = question.getGeneralTemplate().get(language);
            String mergedCode = codeMergerService.mergeCode(userCode, template, language);

            logger.info("========== MERGED CODE START ==========");
            logger.info("{}", mergedCode);
            logger.info("========== MERGED CODE END ==========");

            // 2️⃣ Check compilation (merged code)
            logger.info("Step 2: Checking code compilation...");
            ExecutionResponse compileCheck = pistonService.executeCode(new ExecutionRequest(
                language,
                pistonService.getLanguageVersion(language),
                mergedCode
            ));

            if (compileCheck.hasCompileError()) {
                logger.error("Compilation failed!");
                logger.error("Compile error: {}", compileCheck.getCompile().getStderr());
                result.setSuccess(false);
                result.setVerdict("WRONG_ANSWER");
                result.setError(compileCheck.getCompile().getStderr());
                result.setFailedTestCaseIndex(testcases.get(0).getId());
                return result;
            }
            logger.info("Code compiles successfully!");

            // 3️⃣ Execute with testcases
            logger.info("Step 3: Executing test cases...");
            List<TestCaseResult> testCaseResults = new ArrayList<>();
            long maxTime = 0;
            int passedCount = 0;
            Integer firstFailedIndex = null;
            String finalVerdict = "ACCEPTED";

            for (int i = 0; i < testcases.size(); i++) {
                Question.Testcase testcase = testcases.get(i);
                logger.info("--- Executing test case {} (ID: {}) ---", i + 1, testcase.getId());

                // Prepare input
                String stdin = codeMergerService.formatInput(testcase.getInput(), language);
                logger.info("Input for test case {}: {}", testcase.getId(), stdin);

                ExecutionRequest execRequest = new ExecutionRequest(language,
                    pistonService.getLanguageVersion(language), mergedCode);
                execRequest.setStdin(stdin);

                ExecutionResponse execResponse = pistonService.executeCode(execRequest);

                logger.info("Execution completed for test case {}", testcase.getId());
                logger.info("STDOUT: {}", execResponse.getRun().getStdout());
                logger.info("STDERR: {}", execResponse.getRun().getStderr());
                logger.info("Exit code: {}", execResponse.getRun().getCode());

                TestCaseResult tcResult = new TestCaseResult();
                tcResult.setIndex(testcase.getId());

                if (execResponse.hasRuntimeError()) {
                    logger.error("Runtime error detected for test case {}", testcase.getId());
                    tcResult.setStatus("error");
                    tcResult.setOutput("Runtime Error: " + execResponse.getRun().getStderr());
                    tcResult.setTimeMs(0L);
                    
                    if (firstFailedIndex == null) {
                        firstFailedIndex = testcase.getId();
                        finalVerdict = "WRONG_ANSWER";
                    }
                } else {
                    String output = execResponse.getRun().getStdout().trim();
                    Long timeMs = execResponse.getRun().getWallTime();

                    tcResult.setOutput(output);
                    tcResult.setTimeMs(timeMs);

                    maxTime = Math.max(maxTime, timeMs);

                    // Compare output
                    boolean outputMatch = outputComparatorService.compare(
                        output, 
                        testcase.getExpectedOutput()
                    );

                    logger.info("Output comparison: {}", outputMatch ? "MATCH" : "MISMATCH");
                    logger.info("Expected: {}", testcase.getExpectedOutput());
                    logger.info("Got: {}", output);

                    // Check TLE
                    boolean isTLE = timeMs > testcase.getExpectedTimeLimit();

                    if (!outputMatch) {
                        logger.warn("Output mismatch for test case {}", testcase.getId());
                        tcResult.setStatus("wrong");
                        if (firstFailedIndex == null) {
                            firstFailedIndex = testcase.getId();
                            finalVerdict = "WRONG_ANSWER";
                        }
                    } else if (isTLE) {
                        logger.warn("TLE for test case {}: {}ms > {}ms", testcase.getId(), timeMs, testcase.getExpectedTimeLimit());
                        tcResult.setStatus("tle");
                        if (firstFailedIndex == null) {
                            firstFailedIndex = testcase.getId();
                            finalVerdict = "TLE";
                        }
                    } else {
                        logger.info("Test case {} passed!", testcase.getId());
                        tcResult.setStatus("success");
                        passedCount++;
                    }
                }

                testCaseResults.add(tcResult);

                // Early exit on submit mode if failed
                if (isSubmit && firstFailedIndex != null) {
                    logger.info("Early exit on submit - first failure at test case {}", firstFailedIndex);
                    break;
                }
            }

            // Build metrics
            CodeExecutionResult.ExecutionMetrics metrics = new CodeExecutionResult.ExecutionMetrics();
            metrics.setMaxTimeMs(maxTime);
            metrics.setTotalTestCases(testcases.size());
            metrics.setPassedTestCases(passedCount);

            // Memory from last execution
            if (!testCaseResults.isEmpty()) {
                metrics.setTotalMemoryMb(50.0); // Placeholder
            }

            result.setSuccess(true);
            result.setVerdict(finalVerdict);
            result.setMessage(finalVerdict.equals("ACCEPTED") ? 
                "All test cases passed!" : 
                "Test case " + firstFailedIndex + " failed");
            result.setTestCaseResults(testCaseResults);
            result.setMetrics(metrics);
            result.setFailedTestCaseIndex(firstFailedIndex);

            logger.info("========== EXECUTION COMPLETE ==========");
            logger.info("Final verdict: {}", finalVerdict);
            logger.info("Passed: {}/{}", passedCount, testcases.size());

            return result;

        } catch (Exception e) {
            logger.error("Exception during execution!", e);
            result.setSuccess(false);
            result.setVerdict("WRONG_ANSWER");
            result.setError(e.getMessage());
            return result;
        }
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