// src/main/java/com/algoarena/service/complexity/ComplexityAnalysisService.java

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

            // Build minimal prompt
            String prompt = buildPrompt(request.getCode(), request.getLanguage());

            // Build request body
            Map<String, Object> requestBody = buildRequestBody(prompt);

            // Call Gemini API
            String response = callGeminiApi(requestBody);

            // Parse and return
            return parseComplexityResponse(response);

        } catch (Exception e) {
            log.error("Error analyzing complexity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze complexity: " + e.getMessage());
        }
    }

    private String buildPrompt(String code, String language) {
        if (language != null && !language.trim().isEmpty()) {
            return String.format(
                    "Analyze time and space complexity. Be CONCISE.\n" +
                            "Return ONLY in this EXACT format (no extra text):\n" +
                            "TC: <complexity notation only>\n" +
                            "SC: <complexity notation only>\n" +
                            "n: <one short sentence explaining what n represents>\n\n" +
                            "Language: %s\n" +
                            "Code:\n%s",
                    language, code);
        } else {
            return String.format(
                    "Analyze time and space complexity. Be CONCISE.\n" +
                            "Return ONLY in this EXACT format (no extra text):\n" +
                            "TC: <complexity notation only>\n" +
                            "SC: <complexity notation only>\n" +
                            "n: <one short sentence explaining what n represents>\n\n" +
                            "Code:\n%s",
                    code);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        // Contents array
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        // Generation config for minimal output
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 150);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String callGeminiApi(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = apiUrl + "?key=" + apiKey;

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini API...");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Gemini API call successful");
                return response.getBody();
            } else {
                throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            // ⭐ Enhanced error handling for different HTTP error codes
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Rate limit exceeded (429). Current limit: 10 requests per minute");
                throw new RuntimeException("Rate limit exceeded. Please try again in a minute. (Limit: 10 requests/min)");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("Invalid API key (401)");
                throw new RuntimeException("Invalid API configuration. Please contact support.");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Bad request (400): {}", e.getResponseBodyAsString());
                throw new RuntimeException("Invalid request format. Please check your code and try again.");
            } else {
                log.error("Gemini API error ({}): {}", e.getStatusCode(), e.getMessage());
                throw new RuntimeException("API request failed: " + e.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage());
        }
    }

    private ComplexityAnalysisResponse parseComplexityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Navigate: candidates[0].content.parts[0].text
            String text = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            log.debug("Gemini response text: {}", text);

            // Parse the structured response
            String[] lines = text.trim().split("\n");

            String timeComplexity = null;
            String spaceComplexity = null;
            String complexityDescription = null;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("TC:")) {
                    timeComplexity = line.substring(3).trim();
                } else if (line.startsWith("SC:")) {
                    spaceComplexity = line.substring(3).trim();
                } else if (line.startsWith("n:")) {
                    complexityDescription = line.substring(2).trim();
                } else if (line.toLowerCase().startsWith("where")) {
                    complexityDescription = line.replaceFirst("(?i)^where\\s+", "");
                }
            }

            // Validate we got all required fields
            if (timeComplexity == null || spaceComplexity == null) {
                log.error("Failed to parse complexity from response: {}", text);
                throw new RuntimeException("Invalid response format from Gemini");
            }

            // Default description if not provided
            if (complexityDescription == null || complexityDescription.isEmpty()) {
                complexityDescription = "n is the input size";
            }

            ComplexityAnalysisResponse result = new ComplexityAnalysisResponse(
                    timeComplexity,
                    spaceComplexity,
                    complexityDescription);

            log.info("Successfully parsed complexity: TC={}, SC={}", timeComplexity, spaceComplexity);
            return result;

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage());
        }
    }
}