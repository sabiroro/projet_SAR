package implems;

import abstracts.Channel;
import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;
import java.util.HashMap;
import utils.BrokerManager;
import utils.EventPump;

public class QueueBroker extends abstracts.QueueBroker {

	protected HashMap<Integer, Boolean> bindStates = new HashMap<>();
	
	protected QueueAcceptListener externalQueueAcceptListener;
	protected QueueConnectListener externalQueueConnectListener;
	protected Broker internalBroker;
	
	protected AcceptListener internalAcceptListener = new AcceptListener() {
		@Override
		public void accepted(Channel channel, int port) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: Internal rendez vous Event from accept");
			externalQueueAcceptListener.accepted(new MessageQueue(channel));
			
			if(bindStates.get(port) != null) {
				internalBroker.accept(port, this);
			}
		}
	};
	
	public ConnectListener internalConnectListener = new ConnectListener() {
		@Override
		public void refused() {
			EventPump.log(EventPump.VerboseLevel.LOW_VERBOSE, "QueueBroker: Internal rendez vous Event from connect");
			externalQueueConnectListener.refused();
		}

		@Override
		public void connected(Channel channel) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: Internal rendez vous Event from connect");
			externalQueueConnectListener.connected(new MessageQueue(channel));
		}
		
	};
	
	public QueueBroker(Broker b) {
		EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Queue Broker created");
		this.internalBroker = b;
	}
	
	public QueueBroker(String b) {
		EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Queue Broker created");
		this.internalBroker = new Broker(b);
	}
	
	@Override
	public synchronized boolean bind(int port, QueueAcceptListener listener) {
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: bind " + port);
		if(this.bindStates.get(port) != null) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: refuse the accept on port " + port);
			return false;
		}
		
		// If max connect then refuse
		if(this.internalBroker.connectEvents.get(port) != null && this.internalBroker.connectEvents.get(port).size() >= this.internalBroker.MAX_CONNECTION_SIZE) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "Broker: Refused connect request");
			return false;
		}
		
		this.externalQueueAcceptListener = listener;
		bindStates.put(port, true);

		Task.task().post(()-> this.internalBroker.accept(port, internalAcceptListener), "Accept Task");
		
		return true;
	}

	@Override
	public boolean unbind(int port) {
		if(this.bindStates.get(port) == null) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: refuse unbind on unexisting port " + port);
			return false;
		}
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: unbind " + port);
		bindStates.remove(port);
		//Task.task().kill();
		return true;
	}

	@Override
	public synchronized boolean connect(String name, int port, QueueConnectListener listener) {
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: connecting to " + name + " on port " + port);
		this.externalQueueConnectListener = listener;
		
		abstracts.Broker b = BrokerManager.getInstance().getBroker(name);
		// Signal to the user the action cannot be performed 
		if (b == null) {
			Task.task().post(() -> this.externalQueueConnectListener.refused(), "Internal refused Event");
			return false;
		}

		Task.task().post(()-> this.internalBroker.connect(port, name, internalConnectListener), "Connect Task");
		
		return true;
	}

}
