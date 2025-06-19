package com.novatech.concurqueue;

import com.novatech.concurqueue.demo.SynchronizationDemo;
import com.novatech.concurqueue.system.ConcurQueueSystem;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Main runner class that demonstrates both synchronization challenges
 * and the complete ConcurQueue system functionality.
 */
public class ConcurQueueDemoRunner {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("🚀 CONCURQUEUE COMPREHENSIVE DEMONSTRATION 🚀");
        System.out.println("Choose what you'd like to see:");
        System.out.println("1. Synchronization Challenges Demo");
        System.out.println("2. Full ConcurQueue System Demo");
        System.out.println("3. Both (Sync Demo first, then ConcurQueue)");
        System.out.print("Enter your choice (1-3): ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                runSynchronizationDemo();
                break;
            case "2":
                runConcurQueueDemo();
                break;
            case "3":
                runBothDemos();
                break;
            default:
                System.out.println("Invalid choice. Running both demos...");
                runBothDemos();
        }

        scanner.close();
    }

    private static void runSynchronizationDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RUNNING SYNCHRONIZATION CHALLENGES DEMONSTRATION");
        System.out.println("=".repeat(60));

        SynchronizationDemo.main(new String[]{});

        System.out.println("\nKey Takeaways from Synchronization Demo:");
        System.out.println("• Race conditions occur when multiple threads access shared data unsafely");
        System.out.println("• AtomicInteger and synchronized blocks prevent race conditions");
        System.out.println("• Deadlocks happen when threads wait for each other's locks");
        System.out.println("• Consistent lock ordering prevents deadlocks");
        System.out.println("• ConcurQueue uses these techniques to ensure thread safety");
    }

    private static void runConcurQueueDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RUNNING CONCURQUEUE SYSTEM DEMONSTRATION");
        System.out.println("=".repeat(60));

        ConcurQueueSystem system = new ConcurQueueSystem(
                2,
                5,
                10,
                1,
                5
        );

        Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));

        system.start();

        long runtimeSeconds = 35;
        System.out.printf("System will run for %d seconds. Watch the concurrent behavior!%n", runtimeSeconds);
        System.out.println("Press Ctrl+C to stop early and see graceful shutdown.\n");

        try {
            TimeUnit.SECONDS.sleep(runtimeSeconds);
            system.shutdown();
        } catch (InterruptedException e) {
            System.err.println("Demo interrupted, initiating shutdown...");
            system.shutdown();
            Thread.currentThread().interrupt();
        }

        System.out.println("\nConcurQueue Demo Complete!");
        System.out.println("You observed:");
        System.out.println("• Multiple producers submitting tasks concurrently");
        System.out.println("• Priority-based task processing");
        System.out.println("• Worker threads processing tasks safely");
        System.out.println("• Real-time monitoring and status tracking");
        System.out.println("• Graceful system shutdown with queue draining");
    }

    private static void runBothDemos() {
        runSynchronizationDemo();

        System.out.println("\n" + "⏸".repeat(60));
        System.out.println("SYNCHRONIZATION DEMO COMPLETE");
        System.out.println("Starting ConcurQueue System Demo in 3 seconds...");
        System.out.println("⏸".repeat(60));

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        runConcurQueueDemo();

        System.out.println("\n" + "🎉".repeat(20));
        System.out.println("COMPLETE DEMONSTRATION FINISHED!");
        System.out.println("🎉".repeat(20));
        System.out.println("\nYou've seen:");
        System.out.println("✅ Race conditions and their solutions");
        System.out.println("✅ Deadlock scenarios and prevention");
        System.out.println("✅ Thread-safe concurrent task processing");
        System.out.println("✅ Priority queues and worker pools");
        System.out.println("✅ Real-time monitoring and graceful shutdown");
        System.out.println("\nConcurQueue successfully demonstrates enterprise-grade");
        System.out.println("concurrent programming patterns and best practices!");
    }
}