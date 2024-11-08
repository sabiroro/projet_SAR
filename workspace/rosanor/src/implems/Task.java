package implems;

import utils.EventPump;

public class Task extends abstracts.Task {

	private boolean killed = false;
    private EventToPublish currentEvent;
    
    public class EventToPublish {
    	public String name;
    	public abstracts.Task src; public abstracts.Task dst; Runnable r;
    	public EventToPublish(abstracts.Task src, abstracts.Task dst, Runnable r) {
    		this.src = src; this.dst = dst; this.r = r;
    	}
    	public EventToPublish(abstracts.Task src, abstracts.Task dst, Runnable r, String name) {
    		this.src = src; this.dst = dst; this.r = r; this.name = name;
    	}
    	
    	public void run() {
    		this.r.run();
    	}
    }
    
    public static abstracts.Task task() {
    	abstracts.Task curr = EventPump.getInstance().getCurrentTask();
    	if (curr == null) return new Task(); // For going back to event world
    	return curr;
    }

    @Override
    public void post(Runnable r) {
    	if (!killed) {
    		EventToPublish e = new EventToPublish(task(), this, r);
	        EventPump.getInstance().post(e);
	        this.currentEvent = e;
    	}
    }
    public void post(Runnable r, String name) {
    	if (!killed) {
    		EventToPublish e = new EventToPublish(task(), this, r, name);
	        EventPump.getInstance().post(e);
	        this.currentEvent = e;
    	}
    }

    @Override
    public void kill() {
        // TODO code that function ^^'
    }	

    @Override
    public boolean killed() {
        return killed;
    }
}
