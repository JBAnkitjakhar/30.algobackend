// src/main/java/com/algoarena/service/compiler/QueueService.java
package com.algoarena.service.compiler;

import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueueService {

    private static final int MAX_CONCURRENT_EXECUTIONS = 2;
    private static final int MAX_QUEUE_SIZE = 10;
    private static final long QUEUE_TIMEOUT_SECONDS = 30;

    private final Semaphore executionSemaphore;
    private final ConcurrentHashMap<String, Long> userRequests; // userId -> timestamp
    private final AtomicInteger queueSize;

    public QueueService() {
        this.executionSemaphore = new Semaphore(MAX_CONCURRENT_EXECUTIONS, true);
        this.userRequests = new ConcurrentHashMap<>();
        this.queueSize = new AtomicInteger(0);
    }

    /**
     * Try to acquire execution slot for user
     * @return true if acquired, false if rejected
     */
    public boolean tryAcquire(String userId) throws InterruptedException {
        // Check if user already has a pending request
        if (userRequests.containsKey(userId)) {
            throw new IllegalStateException("You already have a pending request. Please wait.");
        }

        // Check if queue is full
        if (queueSize.get() >= MAX_QUEUE_SIZE) {
            throw new IllegalStateException("Queue is full. Please try again later.");
        }

        // Increment queue size
        queueSize.incrementAndGet();
        userRequests.put(userId, System.currentTimeMillis());

        try {
            // Try to acquire within timeout
            boolean acquired = executionSemaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!acquired) {
                // Timeout exceeded
                throw new TimeoutException("Request timeout. Please try again.");
            }
            
            return true;
            
        } catch (TimeoutException e) {
            throw new IllegalStateException("Queue timeout exceeded. Please try again later.");
        } finally {
            // Always decrement queue size if not acquired
            if (!executionSemaphore.hasQueuedThreads()) {
                queueSize.decrementAndGet();
            }
        }
    }

    /**
     * Release execution slot after completion
     */
    public void release(String userId) {
        executionSemaphore.release();
        userRequests.remove(userId);
        queueSize.decrementAndGet();
    }

    /**
     * Get current queue status
     */
    public QueueStatus getQueueStatus() {
        QueueStatus status = new QueueStatus();
        status.setAvailableSlots(executionSemaphore.availablePermits());
        status.setQueueSize(queueSize.get());
        status.setMaxQueueSize(MAX_QUEUE_SIZE);
        status.setMaxConcurrent(MAX_CONCURRENT_EXECUTIONS);
        return status;
    }

    public static class QueueStatus {
        private int availableSlots;
        private int queueSize;
        private int maxQueueSize;
        private int maxConcurrent;

        // Getters and Setters
        public int getAvailableSlots() { return availableSlots; }
        public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }
        
        public int getQueueSize() { return queueSize; }
        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
        
        public int getMaxQueueSize() { return maxQueueSize; }
        public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
        
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    }
}