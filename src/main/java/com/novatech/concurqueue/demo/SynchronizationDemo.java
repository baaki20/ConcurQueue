package com.novatech.concurqueue.demo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstration of synchronization challenges and their solutions.
 * Shows race conditions, deadlocks, and proper synchronization techniques.
 */
public class SynchronizationDemo {

    // Unsafe counter for race condition demonstration
    private static int unsafeCounter = 0;
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) {
        System.out.println("=== SYNCHRONIZATION CHALLENGES DEMONSTRATION ===\n");

        // 1. Race Condition Demo
        demonstrateRaceCondition();

        // 2. Race Condition Fix Demo
        demonstrateRaceConditionFix();

        // 3. Deadlock Demo
        demonstrateDeadlock();

        // 4. Deadlock Prevention Demo
        demonstrateDeadlockPrevention();
    }

    /**
     * Demonstrates race condition with unsafe counter
     */
    private static void demonstrateRaceCondition() {
        System.out.println("1. RACE CONDITION DEMONSTRATION");
        System.out.println("Starting 10 threads, each incrementing unsafe counter 1000 times...");

        unsafeCounter = 0;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        // Start 10 threads that increment the unsafe counter
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    // RACE CONDITION: Multiple threads accessing unsafeCounter without synchronization
                    unsafeCounter++; // This is NOT thread-safe!
                }
                System.out.printf("Thread-%d finished incrementing%n", threadId);
                latch.countDown();
            });
        }

        try {
            latch.await();
            System.out.printf("Expected result: 10000, Actual result: %d%n", unsafeCounter);
            if (unsafeCounter != 10000) {
                System.out.println("❌ RACE CONDITION DETECTED! Lost updates due to unsafe access.");
            } else {
                System.out.println("✅ No race condition detected this time (but it's still unsafe!)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        System.out.println();
    }

    /**
     * Demonstrates proper synchronization to fix race conditions
     */
    private static void demonstrateRaceConditionFix() {
        System.out.println("2. RACE CONDITION FIX DEMONSTRATION");
        System.out.println("Using AtomicInteger and synchronized blocks...");

        // Solution 1: AtomicInteger
        AtomicInteger atomicCounter = new AtomicInteger(0);

        // Solution 2: Synchronized counter
        SynchronizedCounter syncCounter = new SynchronizedCounter();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20); // 10 threads for each solution

        // Test AtomicInteger
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicCounter.incrementAndGet(); // Thread-safe atomic operation
                }
                System.out.printf("AtomicCounter Thread-%d finished%n", threadId);
                latch.countDown();
            });
        }

        // Test Synchronized counter
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    syncCounter.increment(); // Thread-safe synchronized method
                }
                System.out.printf("SyncCounter Thread-%d finished%n", threadId);
                latch.countDown();
            });
        }

        try {
            latch.await();
            System.out.printf("AtomicInteger result: %d (Expected: 10000)%n", atomicCounter.get());
            System.out.printf("Synchronized result: %d (Expected: 10000)%n", syncCounter.getValue());
            System.out.println("✅ Both solutions prevent race conditions!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        System.out.println();
    }

    /**
     * Demonstrates a classic deadlock scenario
     */
    private static void demonstrateDeadlock() {
        System.out.println("3. DEADLOCK DEMONSTRATION");
        System.out.println("Creating two threads that will deadlock...");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Acquires lock1 then tries to acquire lock2
        executor.submit(() -> {
            synchronized (lock1) {
                System.out.println("Thread-1: Acquired lock1, trying to acquire lock2...");
                try {
                    Thread.sleep(100); // Give Thread-2 time to acquire lock2
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lock2) {
                    System.out.println("Thread-1: Acquired lock2");
                }
            }
        });

        // Thread 2: Acquires lock2 then tries to acquire lock1
        executor.submit(() -> {
            synchronized (lock2) {
                System.out.println("Thread-2: Acquired lock2, trying to acquire lock1...");
                try {
                    Thread.sleep(100); // Give Thread-1 time to acquire lock1
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lock1) {
                    System.out.println("Thread-2: Acquired lock1");
                }
            }
        });

        // Wait for deadlock to occur
        try {
            Thread.sleep(2000);
            System.out.println("❌ DEADLOCK OCCURRED! Threads are waiting for each other.");
            executor.shutdownNow(); // Force shutdown
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }

    /**
     * Demonstrates deadlock prevention using consistent lock ordering
     */
    private static void demonstrateDeadlockPrevention() {
        System.out.println("4. DEADLOCK PREVENTION DEMONSTRATION");
        System.out.println("Using consistent lock ordering to prevent deadlock...");

        final Object lockA = new Object();
        final Object lockB = new Object();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Both threads acquire locks in the same order: lockA first, then lockB
        executor.submit(() -> {
            synchronized (lockA) {
                System.out.println("Thread-1: Acquired lockA");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockB) {
                    System.out.println("Thread-1: Acquired lockB - Task completed!");
                }
            }
            latch.countDown();
        });

        executor.submit(() -> {
            synchronized (lockA) { // Same order: lockA first
                System.out.println("Thread-2: Acquired lockA");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockB) {
                    System.out.println("Thread-2: Acquired lockB - Task completed!");
                }
            }
            latch.countDown();
        });

        try {
            if (latch.await(3, TimeUnit.SECONDS)) {
                System.out.println("✅ No deadlock! Consistent lock ordering prevents deadlock.");
            } else {
                System.out.println("❌ Timeout - possible deadlock or performance issue.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        System.out.println();
        System.out.println("=== DEMONSTRATION COMPLETE ===");
    }

    /**
     * Thread-safe counter using synchronized methods
     */
    private static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized int getValue() {
            return count;
        }
    }
}