// src/main/java/com/algoarena/repository/CourseReadProgressRepository.java
package com.algoarena.repository;

import com.algoarena.model.CourseReadProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseReadProgressRepository extends MongoRepository<CourseReadProgress, String> {
    
    Optional<CourseReadProgress> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
}