// src/main/java/com/algoarena/repository/SubmissionTrackingRepository.java
package com.algoarena.repository;

import com.algoarena.model.SubmissionTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionTrackingRepository extends MongoRepository<SubmissionTracking, String> {
    Optional<SubmissionTracking> findByUserId(String userId);
}