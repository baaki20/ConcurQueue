package com.novatech.concurqueue.tracker;

import com.novatech.concurqueue.model.Task;
import com.novatech.concurqueue.model.TaskStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TaskTracker {
    private final Map<UUID, TaskStatus> taskStatuses;
    private final Map<UUID, Instant> taskStartTimes;

    public TaskTracker() {
        this.taskStatuses = new ConcurrentHashMap<>();
        this.taskStartTimes = new ConcurrentHashMap<>();
    }

    public void trackTask(UUID taskId, TaskStatus status) {
        taskStatuses.put(taskId, status);
        if (status == TaskStatus.PROCESSING) {
            taskStartTimes.put(taskId, Instant.now());
        }
    }

    public Map<UUID, TaskStatus> getAllTaskStatuses() {
        return new ConcurrentHashMap<>(taskStatuses);
    }

    public TaskStatus getTaskStatus(UUID taskId) {
        return taskStatuses.get(taskId);
    }

    public Instant getTaskStartTime(UUID taskId) {
        return taskStartTimes.get(taskId);
    }

    public void updateTaskStatus(UUID id, TaskStatus taskStatus) {
        taskStatuses.put(id, taskStatus);
        if (taskStatus == TaskStatus.PROCESSING) {
            taskStartTimes.put(id, Instant.now());
        }
    }

   public void recordTask(Task task, TaskStatus taskStatus) {
        if (task != null && task.getId() != null) {
            trackTask(task.getId(), taskStatus);
        }
    }
}
