package utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import abstracts.Task;
import implems.Task.EventToPublish;
import utils.EventPump.VerboseLevel;

public class EventPump extends Thread {

	private static EventPump instance;
    private final BlockingQueue<EventToPublish> taskQueue;
    private boolean running;

    // Private constructor to prevent instantiation
    private EventPump() {
        this.setName("EVENT_PUMP");
        this.taskQueue = new LinkedBlockingQueue<>(); // Thread-safe queue
        this.running = true;
    }

    public void stopPump() {
        running = false;
        interrupt(); // Interrupt the thread when stopping
    }

    private EventToPublish currentEvent;

    public Task getCurrentTask() {
        if (this.currentEvent == null) return null;
        return currentEvent.src;
    }

    public enum VerboseLevel {
        HIGH_VERBOSE, 
        MEDIUM_VERBOSE, 
        LOW_VERBOSE, 
        NO_VERBOSE
    }

   
    public static VerboseLevel VERBOSE = VerboseLevel.NO_VERBOSE;
    
    public static void log(VerboseLevel level, String message) {
	    if (EventPump.VERBOSE.ordinal() <= level.ordinal()) {
	        System.out.println(message);
	    }
	}


    @Override
    public void run() {
        while (running) {
            try {
                // Take blocks until a task is available
                currentEvent = taskQueue.take(); 
                EventPump.log(VerboseLevel.HIGH_VERBOSE,"Pump processing : " + currentEvent.name);
                currentEvent.run();
            } catch (InterruptedException e) {
                // If interrupted and we're no longer running, exit loop
                if (!running) {
                    break;
                }
            }
        }
    }

    // Static method to get the single instance of EventPump
    public static synchronized EventPump getInstance() {
        if (instance == null) {
            instance = new EventPump();
        }
        return instance;
    }

    // Posts a task to the queue
    public void post(EventToPublish task) {
        if (!running) return; // Silently drop if the pump is not running
        taskQueue.offer(task); // Non-blocking, thread-safe method to add a task
    }
}
