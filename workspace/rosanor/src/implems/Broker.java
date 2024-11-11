package implems;

import utils.BrokerManager;
import utils.CircularBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public class Broker extends abstracts.Broker {

	public interface AcceptListener {
		void accepted(abstracts.Channel queue);
	}
	public interface ConnectListener {
		void refused();
		void connected(abstracts.Channel queue);
	}
	
	private String name;
	protected static final int BUFFER_SIZE = 10;
	protected static final int MAX_CONNECTION_SIZE = 10000;
	
	protected HashMap<Integer, Request> acceptEvents = new HashMap<Integer, Request>();
	protected HashMap<Integer, ArrayList<Request>> connectEvents = new HashMap<Integer, ArrayList<Request>>();
	
	class Request implements Runnable {
		int port;
		String name;
		ConnectListener cl;
		AcceptListener al;
		boolean isAccept;
		
		CircularBuffer cb_in;
		CircularBuffer cb_out;
		
		public Request(int port, AcceptListener l) {
			this.port = port; this.al = l;
			ArrayList<Request> connectReq = connectEvents.get(this.port);
			if(connectReq != null) {
				if(connectReq.size() > 0) {
							Task.task().post(this, "Rendez vous Event");
				}
			}
		}
		
		public Request(int port, String name, ConnectListener l) {
			this.port = port; this.name = name; this.cl = l;
			if(acceptEvents.get(port) != null) {
				Task.task().post(this, "Rendez vous Event");
			}
		}
		
		@Override
	    public void run() {
			ArrayList<Request> connect_request = connectEvents.get(this.port);
			
			// Someone is asking for a connection so we handle him
			CircularBuffer connect_cb_in = new CircularBuffer(BUFFER_SIZE);
			CircularBuffer connect_cb_out = new CircularBuffer(BUFFER_SIZE);
			
			AtomicBoolean disconnect_monitoring = new AtomicBoolean(false);
			
			Channel connect_channel = new Channel(connect_cb_in, connect_cb_out, disconnect_monitoring);
			
			ConnectListener connect_listener = connect_request.remove(0).cl;
			
			Task.task().post(() -> connect_listener.connected(connect_channel), "Connected Event");
			
			// Then we handle ourselves
			this.cb_in = connect_cb_out;
			this.cb_out = connect_cb_in;
			
			Channel accept_channel = new Channel(cb_in, cb_out, disconnect_monitoring);
			
			Task.task().post(() -> acceptEvents.get(this.port).al.accepted(accept_channel), "Accepted Event");
		}
	}
	
	public Broker(String name) {
		this.name = name;
		// Might want to force user to do that by his own not to leak 'this' reference
		BrokerManager.getInstance().addBroker(this.name, this);
	}

	@Override
	public boolean accept(int port, AcceptListener acl) {
		acceptEvents.put(port, new Request(port, acl));
		return true;
	}


	@Override
	public boolean connect(int port, String name, ConnectListener cnl) {
		if(!this.name.equals(name)) {
			abstracts.Broker b = BrokerManager.getInstance().getBroker(name);
			// Signal to the user the action cannot be performed 
			if (b == null) {
				Task.task().post(() -> cnl.refused(), "Refused Event");
				return false;  
			}
			
			return b.connect(port, name, cnl); 
		}
		
		
		// Add the connect to the list
		ArrayList<Request> listOfRequests = this.connectEvents.get(port);
		if(listOfRequests == null) {
			listOfRequests = new ArrayList<Request>();
		}		
		
		// If max connect then refuse
		if(listOfRequests.size() >= MAX_CONNECTION_SIZE) {
			Task.task().post(() -> cnl.refused(), "Refused Event");
			return false;
		}
		
		listOfRequests.add(new Request(port, name, cnl));
		this.connectEvents.put(port, listOfRequests);
		
		return true;
	}
	

}
