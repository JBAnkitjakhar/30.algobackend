// src/main/java/com/algoarena/interceptor/RateLimitInterceptor.java
package com.algoarena.interceptor;

import com.algoarena.config.RateLimitConfig;
import com.algoarena.exception.RateLimitExceededException;
import com.algoarena.model.User;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("⚠️ No authentication found, skipping rate limit");
            return true;
        }

        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.info("🔍 Rate limit check - Method: {}, URI: {}, User: {}", method, requestURI, userId);

        Bucket bucket = determineBucket(userId, requestURI, method);

        if (bucket == null) {
            logger.info("✅ No rate limit applied for: {}", requestURI);
            return true;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            logger.info("✅ Request allowed. Remaining tokens: {}", probe.getRemainingTokens());
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            logger.warn("❌ RATE LIMIT EXCEEDED for user: {}. Retry after: {} seconds", userId, waitForRefill);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            throw new RateLimitExceededException();
        }
    }

    private Bucket determineBucket(String userId, String requestURI, String method) {

        // ========================================
        // AUTH ENDPOINTS (20/min)
        // ========================================
        if (requestURI.startsWith("/auth/me") ||
                requestURI.startsWith("/auth/refresh")) {
            logger.debug("📌 Matched: AUTH bucket");
            return rateLimitConfig.resolveAuthBucket(userId);
        }

        // ========================================
        // COURSE PROGRESS ENDPOINTS - MUST BE FIRST
        // ========================================
        
        // Course progress write: mark/unmark doc as read (10/min)
        if ((method.equals("POST") || method.equals("PUT")) &&
                requestURI.matches("/api/courses/docs/[^/]+/read")) {
            logger.debug("📌 Matched: COURSE PROGRESS WRITE bucket (10/min)");
            return rateLimitConfig.resolveCourseProgressWriteBucket(userId);
        }

        // Course progress read: get read stats (30/min)
        if (method.equals("GET") && requestURI.equals("/courses/read/stats")) {
            logger.debug("📌 Matched: COURSE PROGRESS READ bucket (30/min)");
            return rateLimitConfig.resolveCourseProgressReadBucket(userId);
        }

        // ========================================
        // COURSE DOCS READ ENDPOINTS (30/min)
        // ========================================
        if (method.equals("GET") && requestURI.startsWith("/courses")) {
            logger.debug("📌 Matched: COURSE DOCS READ bucket (30/min)");
            return rateLimitConfig.resolveCourseDocsReadBucket(userId);
        }

        // ========================================
        // SPECIFIC READ ENDPOINTS (30/min each)
        // ========================================

        // Question read endpoints
        if (method.equals("GET") &&
                (requestURI.matches("/questions/[^/]+") ||
                        requestURI.equals("/questions/metadata"))) {
            logger.debug("📌 Matched: QUESTION READ bucket");
            return rateLimitConfig.resolveQuestionReadBucket(userId);
        }

        // Category read endpoints
        if (method.equals("GET") && requestURI.startsWith("/categories")) {
            logger.debug("📌 Matched: CATEGORY READ bucket");
            return rateLimitConfig.resolveCategoryReadBucket(userId);
        }

        // Solution read endpoints
        if (method.equals("GET") && requestURI.startsWith("/solutions")) {
            logger.debug("📌 Matched: SOLUTION READ bucket");
            return rateLimitConfig.resolveSolutionReadBucket(userId);
        }

        // ========================================
        // APPROACH ENDPOINTS
        // ========================================

        // Approach write endpoints (5/min)
        if ((method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) &&
                requestURI.contains("/approaches")) {
            logger.debug("📌 Matched: APPROACH WRITE bucket");
            return rateLimitConfig.resolveApproachWriteBucket(userId);
        }

        // Approach read endpoints (20/min)
        if (method.equals("GET") && requestURI.contains("/approaches")) {
            logger.debug("📌 Matched: APPROACH READ bucket");
            return rateLimitConfig.resolveApproachReadBucket(userId);
        }

        // ========================================
        // USER MARK/UNMARK OPERATIONS (10/min)
        // ========================================
        if ((method.equals("POST") || method.equals("DELETE")) &&
                requestURI.contains("/user/me/")) {
            logger.debug("📌 Matched: USER WRITE bucket");
            return rateLimitConfig.resolveWriteBucket(userId);
        }

        // ========================================
        // GENERIC FALLBACKS - MUST BE LAST
        // ========================================

        // Generic write endpoints (10/min)
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            logger.debug("📌 Matched: GENERIC WRITE bucket");
            return rateLimitConfig.resolveWriteBucket(userId);
        }

        // Generic read endpoints (60/min)
        if (method.equals("GET")) {
            logger.debug("📌 Matched: GENERIC READ bucket");
            return rateLimitConfig.resolveReadBucket(userId);
        }

        logger.debug("⚠️ No bucket matched");
        return null;
    }
}