// src/main/java/com/algoarena/service/dsa/SubmissionTrackingService.java

package com.algoarena.service.dsa;

import com.algoarena.dto.dsa.SubmissionHistoryDTO;
import com.algoarena.model.SubmissionTracking;
import com.algoarena.repository.SubmissionTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class SubmissionTrackingService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SubmissionTrackingRepository submissionTrackingRepository;

    /**
     * Thread-safe submission tracking using MongoDB atomic operations
     * Handles concurrent submissions from multiple tabs/devices
     * ⭐ Uses UTC date for consistency across all servers
     */
    public void recordSubmission(String userId) {
        // ⭐ CHANGED: Use UTC date instead of server's local timezone
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Try to increment today's count (if today already exists)
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("submissionHistory").elemMatch(Criteria.where("date").is(today)));
        
        Update incrementUpdate = new Update().inc("submissionHistory.$.count", 1);
        
        SubmissionTracking result = mongoTemplate.findAndModify(
                query, 
                incrementUpdate,
                SubmissionTracking.class);

        // If today doesn't exist yet, add new entry
        if (result == null) {
            Query userQuery = new Query(Criteria.where("userId").is(userId));
            Update pushUpdate = new Update()
                    .push("submissionHistory", 
                            new SubmissionTracking.DailySubmission(today, 1));
            
            mongoTemplate.upsert(userQuery, pushUpdate, SubmissionTracking.class);
        }
    }

    /**
     * Get complete submission history for a user
     */
    public SubmissionHistoryDTO getUserSubmissionHistory(String userId) {
        SubmissionTracking tracking = submissionTrackingRepository.findByUserId(userId)
                .orElse(null);
        
        if (tracking == null) {
            // Return empty history if user has no submissions yet
            SubmissionTracking emptyTracking = new SubmissionTracking();
            emptyTracking.setUserId(userId);
            return new SubmissionHistoryDTO(emptyTracking);
        }
        
        return new SubmissionHistoryDTO(tracking);
    }
}