package com.novatech.concurqueue;

import com.novatech.concurqueue.system.ConcurQueueSystem;

import java.util.concurrent.TimeUnit;

public class ConcurQueueApplication {
    private static final int DEFAULT_NUM_PRODUCERS = 2;
    private static final int DEFAULT_NUM_WORKERS = 5;
    private static final int DEFAULT_TASKS_PER_PRODUCER = 10;
    private static final long DEFAULT_PRODUCER_INTERVAL_SECONDS = 1;
    private static final long DEFAULT_MONITOR_INTERVAL_SECONDS = 5;
    private static final long ADDITIONAL_RUNTIME_SECONDS = 10;

    public static void main(String[] args) {
        ConcurQueueSystem system = new ConcurQueueSystem(
            DEFAULT_NUM_PRODUCERS,
            DEFAULT_NUM_WORKERS,
            DEFAULT_TASKS_PER_PRODUCER,
            DEFAULT_PRODUCER_INTERVAL_SECONDS,
            DEFAULT_MONITOR_INTERVAL_SECONDS
        );
        
        Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));

        system.start();
        runSystemAndAwaitCompletion();
    }

    private static void runSystemAndAwaitCompletion() {
        long totalRuntime = calculateTotalRuntime();
        System.out.printf("System will run for approximately %d seconds%n", totalRuntime);

        try {
            TimeUnit.SECONDS.sleep(totalRuntime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted, initiating shutdown");
        }
    }

    private static long calculateTotalRuntime() {
        return DEFAULT_NUM_PRODUCERS * DEFAULT_TASKS_PER_PRODUCER * 
               DEFAULT_PRODUCER_INTERVAL_SECONDS + ADDITIONAL_RUNTIME_SECONDS;
    }
}
