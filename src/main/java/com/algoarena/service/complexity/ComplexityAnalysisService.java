package com.algoarena.service.complexity;

import com.algoarena.dto.complexity.ComplexityAnalysisRequest;
import com.algoarena.dto.complexity.ComplexityAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComplexityAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ComplexityAnalysisService.class);

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.api-url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public ComplexityAnalysisResponse analyzeComplexity(ComplexityAnalysisRequest request) {
        try {
            log.info("Starting complexity analysis for {} code",
                    request.getLanguage() != null ? request.getLanguage() : "unknown");

            Map<String, Object> requestBody = buildRequestBody(request.getCode(), request.getLanguage());
            String rawResponse = callGeminiApi(requestBody);
            
            return parseComplexityResponse(rawResponse);

        } catch (RuntimeException e) {
            // Re-throw user-friendly errors
            throw e;
        } catch (Exception e) {
            log.error("CRITICAL ERROR in analyzeComplexity: {}", e.getMessage());
            throw new RuntimeException("Something went wrong. Please try again.");
        }
    }

    private Map<String, Object> buildRequestBody(String code, String language) {
        Map<String, Object> requestBody = new HashMap<>();

        // 1. System Instruction: Expert persona with strict rules
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", 
            "You are a Senior DSA Instructor. Analyze code for Time and Space complexity.\n\n" +
            "RULES:\n" +
            "1. Be highly accurate. For Space Complexity, ALWAYS account for: recursion stacks, auxiliary arrays, hash sets.\n" +
            "2. If variables like 'n' or 'm' are used in Big O notation, you MUST define them.\n" +
            "3. Output ONLY a JSON object with: timeComplexity, spaceComplexity, complexityDescription.\n" +
            "4. complexityDescription should ONLY contain variable definitions (max 20 words). Example: 'n is number of rows, m is number of columns'.\n" +
            "5. NO algorithm explanations, NO step-by-step breakdown."
        )));
        requestBody.put("system_instruction", systemInstruction);

        // 2. User Content
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", 
            "Analyze complexity for this " + (language != null ? language : "") + " code:\n" + code
        )));
        requestBody.put("contents", List.of(content));

        // 3. Generation Config - Optimized for cost
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.0);
        generationConfig.put("maxOutputTokens", 800); // Reasonable limit
        generationConfig.put("response_mime_type", "application/json"); 
        
        // Disable internal thinking to save tokens
        Map<String, Object> thinkingConfig = new HashMap<>();
        thinkingConfig.put("thinking_budget", 0);
        generationConfig.put("thinking_config", thinkingConfig);

        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String callGeminiApi(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String url = apiUrl + "?key=" + apiKey;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Rate limit exceeded (429)");
                throw new RuntimeException("Rate limit exceeded. Please try again in a minute.");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("Invalid API key (401)");
                throw new RuntimeException("Invalid API configuration. Please contact support.");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Bad request (400): {}", e.getResponseBodyAsString());
                throw new RuntimeException("Invalid request. Please check your code and try again.");
            } else {
                log.error("Gemini API error ({}): {}", e.getStatusCode(), e.getMessage());
                throw new RuntimeException("API request failed. Please try again.");
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to AI service. Please try again.");
        }
    }

    private ComplexityAnalysisResponse parseComplexityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Log token usage for monitoring
            JsonNode usage = root.path("usageMetadata");
            int inputTokens = usage.path("promptTokenCount").asInt();
            int outputTokens = usage.path("candidatesTokenCount").asInt();
            log.info("Tokens - Input: {}, Output: {}, Total: {}", 
                inputTokens, outputTokens, (inputTokens + outputTokens));

            // Extract JSON text from response
            String jsonText = root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

            if (jsonText.isEmpty()) {
                log.error("Empty response from Gemini");
                throw new RuntimeException("Unable to analyze complexity. Please try again.");
            }

            // Parse complexity data
            JsonNode data = objectMapper.readTree(jsonText.trim());

            String timeComplexity = data.path("timeComplexity").asText();
            String spaceComplexity = data.path("spaceComplexity").asText();
            String description = data.path("complexityDescription").asText();

            // Validate required fields
            if (timeComplexity.isEmpty() || spaceComplexity.isEmpty()) {
                log.error("Missing required fields in response");
                throw new RuntimeException("Incomplete analysis. Please try again.");
            }

            log.info("Successfully analyzed: TC={}, SC={}", timeComplexity, spaceComplexity);

            return new ComplexityAnalysisResponse(
                timeComplexity,
                spaceComplexity,
                description.isEmpty() ? null : description
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("PARSING FAILED. Error: {}", e.getMessage());
            throw new RuntimeException("Failed to process AI response. Please try again.");
        }
    }
}