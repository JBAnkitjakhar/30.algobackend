// src/main/java/com/algoarena/config/MongoConfig.java
package com.algoarena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        
        // ⭐ LocalDateTime converters (existing - keep these)
        converters.add(new LocalDateTimeToDateConverter());
        converters.add(new DateToLocalDateTimeConverter());
        
        // ⭐ LocalDate converters (NEW - for submission tracking)
        converters.add(new LocalDateToStringConverter());
        converters.add(new StringToLocalDateConverter());
        
        return new MongoCustomConversions(converters);
    }

    // ===== LocalDateTime Converters (Existing) =====
    
    @WritingConverter
    static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.toInstant(ZoneOffset.UTC));
        }
    }

    @ReadingConverter
    static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            return LocalDateTime.ofInstant(source.toInstant(), ZoneOffset.UTC);
        }
    }

    // ===== LocalDate Converters (NEW) =====
    
    /**
     * ⭐ Convert LocalDate → String when writing to MongoDB
     * Stores as "YYYY-MM-DD" format (timezone-independent)
     */
    @WritingConverter
    static class LocalDateToStringConverter implements Converter<LocalDate, String> {
        @Override
        public String convert(LocalDate source) {
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE); // "2026-01-30"
        }
    }

    /**
     * ⭐ Convert String → LocalDate when reading from MongoDB
     * Reads "YYYY-MM-DD" format
     */
    @ReadingConverter
    static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}