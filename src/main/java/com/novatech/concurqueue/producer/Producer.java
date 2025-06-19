package com.novatech.concurqueue.producer;

import com.novatech.concurqueue.model.Task;
import com.novatech.concurqueue.model.TaskStatus;
import com.novatech.concurqueue.tracker.TaskTracker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class Producer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Producer.class);
    
    private final BlockingQueue<Task> taskQueue;
    private final TaskTracker taskTracker;
    private final String producerName;
    private final int tasksToProduce;
    private final long productionIntervalSeconds;

    @Override
    public void run() {
        log.info("Producer {} started. Will produce {} tasks every {} seconds.", 
                producerName, tasksToProduce, productionIntervalSeconds);
                
        try {
            for (int i = 0; i < tasksToProduce && !Thread.currentThread().isInterrupted(); i++) {
                Task task = createTask(i + 1);
                taskQueue.put(task);
                taskTracker.recordTask(task, TaskStatus.SUBMITTED);

                log.info("Producer {} submitted task {} (Priority: {}). Queue size: {}", 
                        producerName, 
                        task.getId().toString().substring(0, 8), 
                        task.getPriority(), 
                        taskQueue.size());

                TimeUnit.SECONDS.sleep(productionIntervalSeconds);
            }
            log.info("Producer {} finished producing {} tasks.", producerName, tasksToProduce);
        } catch (InterruptedException e) {
            log.warn("Producer {} was interrupted.", producerName);
            Thread.currentThread().interrupt();
        }
    }

    private Task createTask(int taskNumber) {
        if (taskNumber % 3 == 0) {
            return new Task("HighPriorityTask-" + producerName + "-" + taskNumber, 1,
                    "Payload for High Priority Task " + taskNumber);
        } else if (taskNumber % 5 == 0) {
            return new Task("MediumPriorityTask-" + producerName + "-" + taskNumber, 5,
                    "Payload for Medium Priority Task " + taskNumber);
        }
        return new Task("LowPriorityTask-" + producerName + "-" + taskNumber, 10,
                "Payload for Low Priority Task " + taskNumber);
    }
}
