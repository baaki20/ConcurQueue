package com.novatech.concurqueue.model;

import lombok.Value;
import lombok.EqualsAndHashCode;
import java.time.Instant;
import java.util.UUID;

@Value
@EqualsAndHashCode(of = "id")
public class Task implements Comparable<Task> {
    UUID id = UUID.randomUUID();
    String name;
    int priority;
    Instant createdTimestamp = Instant.now();
    String payload;

    @Override
    public int compareTo(Task other) {
        return Integer.compare(this.priority, other.priority);
    }
}