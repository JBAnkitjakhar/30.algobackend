// src/main/java/com/algoarena/config/WebConfig.java
package com.algoarena.config;

import com.algoarena.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/courses/**",      // ✅ Match controller path
                        "/questions/**",    
                        "/categories/**",   
                        "/solutions/**",    
                        "/approaches/**",   
                        "/user/me/**",      
                        "/auth/me",         
                        "/auth/refresh");
    }
}