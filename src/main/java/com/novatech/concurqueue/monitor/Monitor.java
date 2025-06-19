package com.novatech.concurqueue.monitor;

import com.novatech.concurqueue.model.TaskStatus;
import com.novatech.concurqueue.tracker.TaskTracker;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Monitor implements Runnable {
    private static final String SEPARATOR = "\n--- System Status Report ---";
    private static final String SEPARATOR_END = "----------------------------\n";
    private static final int TASK_ID_DISPLAY_LENGTH = 8;
    private static final long STALLED_TASK_THRESHOLD_MILLIS = 5000;
    private static final String LOG_FORMAT = "[%s] %s%n";
    
    private final BlockingQueue<?> taskQueue;
    private final TaskTracker taskTracker;
    private final AtomicInteger tasksProcessedCount;
    private final ExecutorService workerPool;
    private final long monitoringIntervalSeconds;

    public Monitor(BlockingQueue<?> taskQueue, 
                  TaskTracker taskTracker, 
                  AtomicInteger tasksProcessedCount, 
                  ExecutorService workerPool, 
                  long monitoringIntervalSeconds) {
        this.taskQueue = taskQueue;
        this.taskTracker = taskTracker;
        this.tasksProcessedCount = tasksProcessedCount;
        this.workerPool = workerPool;
        this.monitoringIntervalSeconds = monitoringIntervalSeconds;
    }

    @Override
    public void run() {
        logMessage("Monitor started. Reporting every " + monitoringIntervalSeconds + " seconds.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TimeUnit.SECONDS.sleep(monitoringIntervalSeconds);
                logSystemStatus();
            }
        } catch (InterruptedException e) {
            logMessage("Monitor interrupted and shutting down.");
            Thread.currentThread().interrupt();
        }
    }

    private void logSystemStatus() {
        StringBuilder report = new StringBuilder(SEPARATOR);
        appendBasicMetrics(report);
        appendWorkerPoolMetrics(report);
        appendTaskStatusBreakdown(report);
        appendStalledTasksWarnings(report);
        report.append(SEPARATOR_END);
        System.out.print(report);
    }

    private void appendBasicMetrics(StringBuilder report) {
        report.append(String.format("%nTimestamp: %s", Instant.now()))
              .append(String.format("%nCurrent Queue Size: %d", taskQueue.size()))
              .append(String.format("%nTotal Tasks Processed: %d%n", tasksProcessedCount.get()));
    }

    private void appendWorkerPoolMetrics(StringBuilder report) {
        if (workerPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) workerPool;
            report.append(String.format("Worker Pool Active Threads: %d%n", tpe.getActiveCount()))
                  .append(String.format("Worker Pool Completed Tasks: %d%n", tpe.getCompletedTaskCount()))
                  .append(String.format("Worker Pool Queue Size: %d%n", tpe.getQueue().size()));
        }
    }

    private void appendTaskStatusBreakdown(StringBuilder report) {
        Map<TaskStatus, Long> statusCounts = taskTracker.getAllTaskStatuses().values().stream()
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        
        report.append("Task Status Breakdown:%n");
        for (TaskStatus status : TaskStatus.values()) {
            report.append(String.format("  - %s: %d%n", status, statusCounts.getOrDefault(status, 0L)));
        }
    }

    private void appendStalledTasksWarnings(StringBuilder report) {
        long currentTimeMillis = System.currentTimeMillis();
        taskTracker.getAllTaskStatuses().forEach((taskId, status) -> {
            if (isTaskStalled(taskId, status, currentTimeMillis)) {
                report.append(formatStalledTaskWarning(taskId));
            }
        });
    }

    private boolean isTaskStalled(UUID taskId, TaskStatus status, long currentTimeMillis) {
        if (status != TaskStatus.PROCESSING) {
            return false;
        }
        Instant startTime = taskTracker.getTaskStartTime(taskId);
        return startTime != null &&
               (currentTimeMillis - startTime.toEpochMilli()) > STALLED_TASK_THRESHOLD_MILLIS;
    }

    private String formatStalledTaskWarning(UUID taskId) {
        return String.format("  WARNING: Task %s appears stalled (PROCESSING for >%dms)%n",
                taskId.toString().substring(0, TASK_ID_DISPLAY_LENGTH),
                STALLED_TASK_THRESHOLD_MILLIS);
    }

    private void logMessage(String message) {
        System.out.printf(LOG_FORMAT, Thread.currentThread().getName(), message);
    }
}