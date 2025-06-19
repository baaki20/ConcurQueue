package com.novatech.concurqueue.system;

import com.novatech.concurqueue.model.Task;
import com.novatech.concurqueue.monitor.Monitor;
import com.novatech.concurqueue.producer.Producer;
import com.novatech.concurqueue.tracker.TaskTracker;
import com.novatech.concurqueue.worker.Worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurQueueSystem {
    private static final int PRODUCER_SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int WORKER_SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final int MONITOR_SHUTDOWN_TIMEOUT_SECONDS = 3;
    private static final int QUEUE_DRAIN_CHECK_INTERVAL_SECONDS = 2;
    private static final int TASK_ID_LENGTH = 8;

    public final BlockingQueue<Task> taskQueue;
    private final TaskTracker taskTracker;
    public final ExecutorService producerPool;
    public final ExecutorService workerPool;
    public final ExecutorService monitorPool;
    private final AtomicInteger tasksProcessedCount;

    public ConcurQueueSystem(int numProducers, int numWorkers, int tasksPerProducer, long producerIntervalSeconds, long monitoringIntervalSeconds) {
        this.taskQueue = new PriorityBlockingQueue<>();
        this.taskTracker = new TaskTracker();
        this.tasksProcessedCount = new AtomicInteger(0);

        ThreadFactory producerThreadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Producer-" + counter.getAndIncrement());
                return thread;
            }
        };

        ThreadFactory workerThreadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Worker-" + counter.getAndIncrement());
                return thread;
            }
        };

        ThreadFactory monitorThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Monitor-Thread");
                return thread;
            }
        };

        this.producerPool = Executors.newFixedThreadPool(numProducers, producerThreadFactory);
        this.workerPool = Executors.newFixedThreadPool(numWorkers, workerThreadFactory);
        this.monitorPool = Executors.newSingleThreadExecutor(monitorThreadFactory);

        initializeProducers(numProducers, tasksPerProducer, producerIntervalSeconds);
        initializeWorkers(numWorkers);
        initializeMonitor(monitoringIntervalSeconds);
    }

    private void initializeProducers(int numProducers, int tasksPerProducer, long producerIntervalSeconds) {
        for (int i = 0; i < numProducers; i++) {
            producerPool.submit(new Producer(taskQueue, taskTracker, "Producer-" + (i + 1),
                    tasksPerProducer, producerIntervalSeconds));
        }
    }

    private void initializeWorkers(int numWorkers) {
        for (int i = 0; i < numWorkers; i++) {
            workerPool.submit(new Worker(taskQueue, taskTracker, tasksProcessedCount));
        }
    }

    private void initializeMonitor(long monitoringIntervalSeconds) {
        monitorPool.submit(new Monitor(taskQueue, taskTracker, tasksProcessedCount, workerPool, monitoringIntervalSeconds));
    }

    public void start() {
        System.out.println("ConcurQueue System started. Producers, Workers, and Monitor are active.");
    }

    public void shutdown() {
        System.out.println("Initiating ConcurQueue System shutdown...");
        shutdownMonitor();
        shutdownProducers();
        drainTaskQueue();
        shutdownWorkers();
        printFinalStatus();
    }

    private void shutdownMonitor() {
        monitorPool.shutdownNow();
        awaitTermination(monitorPool, MONITOR_SHUTDOWN_TIMEOUT_SECONDS, "Monitor");
    }

    private void shutdownProducers() {
        producerPool.shutdownNow();
        awaitTermination(producerPool, PRODUCER_SHUTDOWN_TIMEOUT_SECONDS, "Producer");
    }

    private void shutdownWorkers() {
        workerPool.shutdown();
        if (!awaitTermination(workerPool, WORKER_SHUTDOWN_TIMEOUT_SECONDS, "Worker")) {
            workerPool.shutdownNow();
        }
    }

    private boolean awaitTermination(ExecutorService pool, int timeoutSeconds, String poolName) {
        try {
            System.out.println("Attempting to shut down " + poolName + " pool...");
            if (!pool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.err.println(poolName + " pool did not terminate gracefully within " + timeoutSeconds + " seconds.");
                return false;
            }
            System.out.println(poolName + " pool terminated successfully.");
            return true;
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted while waiting for " + poolName.toLowerCase() + "s.");
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void drainTaskQueue() {
        System.out.println("Draining remaining tasks from queue before worker shutdown...");
        while (!taskQueue.isEmpty()) {
            System.out.printf("Queue size: %d. Waiting for tasks to be processed...%n", taskQueue.size());
            try {
                TimeUnit.SECONDS.sleep(QUEUE_DRAIN_CHECK_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Queue is empty. Proceeding with worker shutdown.");
    }

    private void printFinalStatus() {
        System.out.printf("ConcurQueue System shutdown complete. Total tasks processed: %d%n",
                tasksProcessedCount.get());
        System.out.println("Final task statuses:");
        taskTracker.getAllTaskStatuses().forEach((uuid, status) ->
                System.out.printf("  Task %s: %s%n", uuid.toString().substring(0, TASK_ID_LENGTH), status)
        );
    }
}