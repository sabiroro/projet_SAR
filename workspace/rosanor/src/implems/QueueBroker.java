package implems;

import abstracts.Channel;
import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;
import utils.EventPump;

public class QueueBroker extends abstracts.QueueBroker {

	protected boolean isBind;
	
	protected QueueAcceptListener externalQueueAcceptListener;
	protected QueueConnectListener externalQueueConnectListener;
	
	protected AcceptListener internalAcceptListener = new AcceptListener() {

		@Override
		public void accepted(Channel channel) {
			EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: Internal rendez vous Event from accept");
			externalQueueAcceptListener.accepted(new MessageQueue(channel));
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
	
	protected Broker internalBroker;
	public QueueBroker(Broker b) {
		EventPump.log(EventPump.VerboseLevel.HIGH_VERBOSE, "Queue Broker created");
		this.internalBroker = b;
	}
	
	@Override
	public boolean bind(int port, QueueAcceptListener listener) {
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: bind " + port);
		this.externalQueueAcceptListener = listener;
		this.internalBroker.accept(port, internalAcceptListener);
		return false;
	}

	@Override
	public boolean unbind(int port) {
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: unbind " + port);
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(String name, int port, QueueConnectListener listener) {
		EventPump.log(EventPump.VerboseLevel.MEDIUM_VERBOSE, "QueueBroker: connecting to " + name + " on port " + port);
		this.externalQueueConnectListener = listener;
		return this.internalBroker.connect(port, name, internalConnectListener);
	}

}
