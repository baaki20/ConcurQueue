```mermaid
flowchart TD
    %% Start and System Initialization
    Start([System Start]) --> Init[Initialize ConcurQueue System]
    Init --> CreateQueues[Create PriorityBlockingQueue<br/>& TaskTracker]
    CreateQueues --> CreatePools[Create Thread Pools:<br/>- Producer Pool<br/>- Worker Pool<br/>- Monitor Pool]
    CreatePools --> StartSystem[Start All Components]

    %% Producer Flow
    StartSystem --> ProducerStart[Producer Threads Start]
    ProducerStart --> ProducerLoop{Producer Loop<br/>Tasks Remaining?}
    ProducerLoop -->|Yes| CreateTask[Create Task with Priority:<br/>- Every 3rd: High Priority (1)<br/>- Every 5th: Medium Priority (5)<br/>- Others: Low Priority (10)]
    CreateTask --> SubmitTask[Submit Task to<br/>PriorityBlockingQueue]
    SubmitTask --> TrackSubmitted[TaskTracker.recordTask<br/>Status: SUBMITTED]
    TrackSubmitted --> LogSubmission[Log Task Submission<br/>with Queue Size]
    LogSubmission --> ProducerSleep[Sleep for<br/>Production Interval]
    ProducerSleep --> ProducerLoop
    ProducerLoop -->|No| ProducerComplete[Producer Complete]

    %% Worker Flow
    StartSystem --> WorkerStart[Worker Threads Start]
    WorkerStart --> WorkerLoop[Worker Loop<br/>Wait for Tasks]
    WorkerLoop --> TakeTask[BlockingQueue.take()<br/>🔒 Synchronization Point]
    TakeTask --> UpdateProcessing[TaskTracker.updateStatus<br/>Status: PROCESSING<br/>Record Start Time]
    UpdateProcessing --> LogProcessing[Log Processing Start<br/>with Thread Name]
    LogProcessing --> SimulateWork[Simulate Task Processing<br/>Random Delay: 500-2000ms]
    SimulateWork --> FailureCheck{Simulate Failure<br/>10% Chance}
    
    %% Failure and Retry Logic
    FailureCheck -->|Failure| RetryCheck{Retry Count<br/>< 3?}
    RetryCheck -->|Yes| RequeueTask[Re-queue Task<br/>Status: RETRIED]
    RequeueTask --> WorkerLoop
    RetryCheck -->|No| MarkFailed[Mark Task as FAILED<br/>Give Up]
    MarkFailed --> WorkerLoop
    
    %% Success Path
    FailureCheck -->|Success| CompleteTask[Mark Task as COMPLETED<br/>Increment Counter]
    CompleteTask --> LogCompletion[Log Task Completion]
    LogCompletion --> WorkerLoop

    %% Monitor Flow
    StartSystem --> MonitorStart[Monitor Thread Start]
    MonitorStart --> MonitorLoop[Monitor Loop<br/>Every 5 seconds]
    MonitorLoop --> CollectMetrics[Collect System Metrics:<br/>- Queue Size<br/>- Active Threads<br/>- Task Status Counts]
    CollectMetrics --> CheckStalled[Check for Stalled Tasks<br/>Processing > 5 seconds]
    CheckStalled --> LogReport[Log Comprehensive<br/>System Status Report]
    LogReport --> MonitorSleep[Sleep for<br/>Monitor Interval]
    MonitorSleep --> MonitorContinue{System<br/>Running?}
    MonitorContinue -->|Yes| MonitorLoop
    MonitorContinue -->|No| MonitorShutdown[Monitor Shutdown]

    %% Shutdown Flow
    ProducerComplete --> CheckShutdown{All Producers<br/>Complete?}
    CheckShutdown -->|No| WaitProducers[Wait for Other<br/>Producers]
    WaitProducers --> CheckShutdown
    CheckShutdown -->|Yes| InitiateShutdown[Initiate System Shutdown]
    InitiateShutdown --> ShutdownMonitor[Shutdown Monitor Pool<br/>⏱️ 3s Timeout]
    ShutdownMonitor --> ShutdownProducers[Shutdown Producer Pool<br/>⏱️ 5s Timeout]
    ShutdownProducers --> DrainQueue[Drain Task Queue<br/>Wait for Workers to Complete]
    DrainQueue --> QueueEmpty{Queue<br/>Empty?}
    QueueEmpty -->|No| WaitDrain[Wait 2 seconds<br/>Check Again]
    WaitDrain --> DrainQueue
    QueueEmpty -->|Yes| ShutdownWorkers[Shutdown Worker Pool<br/>⏱️ 30s Timeout]
    ShutdownWorkers --> FinalReport[Print Final Status Report<br/>All Task Statuses]
    FinalReport --> SystemEnd([System Shutdown Complete])

    %% Concurrent Data Structures
    subgraph "Shared Data Structures 🔒"
        PriorityQueue[PriorityBlockingQueue&lt;Task&gt;<br/>Thread-Safe Priority Queue]
        TaskTracker[TaskTracker<br/>ConcurrentHashMap&lt;UUID, TaskStatus&gt;<br/>ConcurrentHashMap&lt;UUID, Instant&gt;]
        AtomicCounter[AtomicInteger<br/>tasksProcessedCount]
    end

    %% Synchronization Points
    subgraph "Synchronization Points 🔒"
        SP1[Queue Operations<br/>put() / take()]
        SP2[TaskTracker Updates<br/>ConcurrentHashMap]
        SP3[Counter Increment<br/>AtomicInteger]
        SP4[Monitor Data Collection<br/>Thread-Safe Reads]
    end

    %% Thread Pool Management
    subgraph "Thread Pool Management"
        ProducerPool[Producer Thread Pool<br/>Fixed Size: 2]
        WorkerPool[Worker Thread Pool<br/>Fixed Size: 5]
        MonitorPool[Monitor Thread Pool<br/>Single Thread]
    end

    %% Add color coding for different components
    classDef producer fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef worker fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef monitor fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef system fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef sync fill:#ffebee,stroke:#c62828,stroke-width:2px

    %% Apply styles
    class ProducerStart,ProducerLoop,CreateTask,SubmitTask,ProducerSleep,ProducerComplete producer
    class WorkerStart,WorkerLoop,TakeTask,UpdateProcessing,SimulateWork,CompleteTask worker
    class MonitorStart,MonitorLoop,CollectMetrics,CheckStalled,LogReport,MonitorSleep monitor
    class Init,CreateQueues,CreatePools,StartSystem,InitiateShutdown,DrainQueue,FinalReport system
    class PriorityQueue,TaskTracker,AtomicCounter,SP1,SP2,SP3,SP4 sync
```