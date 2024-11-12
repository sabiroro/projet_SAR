package implems;

import utils.EventPump;

public class Task extends abstracts.Task {

	private boolean killed = false;
    private EventToPublish currentEvent;
    
    public class EventToPublish {
    	public String name;
    	public abstracts.Task src; public abstracts.Task dst; Runnable r;
    	public EventToPublish(abstracts.Task src, abstracts.Task dst, Runnable r) {
			EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Task: creating event to publish with name " + name);
    		this.src = src; this.dst = dst; this.r = r;
    	}
    	public EventToPublish(abstracts.Task src, abstracts.Task dst, Runnable r, String name) {
			EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Task: creating event to publish with name " + name);
    		this.src = src; this.dst = dst; this.r = r; this.name = name;
    	}
    	
    	public void run() {
    		this.r.run();
    	}
    }
    
    public static abstracts.Task task() {
		EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Task: getting current task");
    	abstracts.Task curr = EventPump.getInstance().getCurrentTask();
    	if (curr == null) return new Task(); // For going back to event world
    	return curr;
    }

    public void post(Runnable r, String name) {
    	if (!killed) {
			EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Task: posting event with name " + name);
    		EventToPublish e = new EventToPublish(task(), this, r, name);
	        EventPump.getInstance().post(e);
	        this.currentEvent = e;
    	}
    }

    @Override
    public void kill() {
        // TODO code that function ^^'
		EventPump.log(EventPump.VerboseLevel.LOW_VERBOSE, "Task: killing task");
		killed = true;
    }	

    @Override
    public boolean killed() {
        return killed;
    }
}
