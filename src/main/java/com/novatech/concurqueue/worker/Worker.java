package com.novatech.concurqueue.worker;

import com.novatech.concurqueue.model.Task;
import com.novatech.concurqueue.model.TaskStatus;
import com.novatech.concurqueue.tracker.TaskTracker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class Worker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);
    private static final int MAX_RETRIES = 3;
    private static final double FAILURE_RATE = 0.1;
    private static final int MIN_PROCESSING_TIME = 500;
    private static final int MAX_PROCESSING_TIME = 2000;

    private final BlockingQueue<Task> taskQueue;
    private final TaskTracker taskTracker;
    private final AtomicInteger tasksProcessedCount;

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processTask(taskQueue.take(), 0);
            }
        } catch (InterruptedException e) {
            log.warn("Worker interrupted and shutting down.");
            Thread.currentThread().interrupt();
        }
    }

    private void processTask(Task task, int retryCount) throws InterruptedException {
        String taskId = task.getId().toString().substring(0, 8);
        log.info("Starting task {} (Priority: {}, Retries: {})", taskId, task.getPriority(), retryCount);
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.PROCESSING);

        try {
            simulateTaskProcessing();

            if (shouldSimulateFailure() && retryCount < MAX_RETRIES) {
                handleTaskRetry(task, retryCount);
                return;
            }

            completeTask(task);
        } catch (InterruptedException e) {
            handleTaskInterruption(task);
            throw e;
        } catch (Exception e) {
            handleTaskError(task, retryCount, e);
        }
    }

    private void simulateTaskProcessing() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(MIN_PROCESSING_TIME, MAX_PROCESSING_TIME));
    }

    private boolean shouldSimulateFailure() {
        return ThreadLocalRandom.current().nextDouble() < FAILURE_RATE;
    }

    private void handleTaskRetry(Task task, int retryCount) throws InterruptedException {
        String taskId = task.getId().toString().substring(0, 8);
        log.warn("Simulating failure for task {}. Re-queueing...", taskId);
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.FAILED);
        taskQueue.put(task);
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.RETRIED);
    }

    private void completeTask(Task task) {
        log.info("Completed task {}", task.getId().toString().substring(0, 8));
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);
        tasksProcessedCount.incrementAndGet();
    }

    private void handleTaskInterruption(Task task) {
        log.warn("Interrupted during processing of task {}", task.getId().toString().substring(0, 8));
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.FAILED);
    }

    private void handleTaskError(Task task, int retryCount, Exception e) throws InterruptedException {
        String taskId = task.getId().toString().substring(0, 8);
        log.error("Error processing task {}: {}", taskId, e.getMessage());
        taskTracker.updateTaskStatus(task.getId(), TaskStatus.FAILED);

        if (retryCount < MAX_RETRIES) {
            log.info("Re-queueing failed task {} for retry {}", taskId, retryCount + 1);
            taskQueue.put(task);
            taskTracker.updateTaskStatus(task.getId(), TaskStatus.RETRIED);
        } else {
            log.error("Task {} failed after {} retries. Giving up.", taskId, MAX_RETRIES);
        }
    }
}
