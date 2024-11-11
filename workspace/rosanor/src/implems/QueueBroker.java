package implems;

import abstracts.Channel;
import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;

public class QueueBroker extends abstracts.QueueBroker {

	protected boolean isBind;
	
	protected QueueAcceptListener externalQueueAcceptListener;
	protected QueueConnectListener externalQueueConnectListener;
	
	protected AcceptListener internalAcceptListener = new AcceptListener() {

		@Override
		public void accepted(Channel channel) {
			System.out.println("Internaly Accepted");
			externalQueueAcceptListener.accepted(new MessageQueue(channel));
		}

	};
	
	public ConnectListener internalConnectListener = new ConnectListener() {

		@Override
		public void refused() {
			System.out.println("Internaly Refused");
			externalQueueConnectListener.refused();
		}

		@Override
		public void connected(Channel channel) {
			System.out.println("Internaly Connected");
			externalQueueConnectListener.connected(new MessageQueue(channel));
		}
		
	};
	
	protected Broker internalBroker;
	public QueueBroker(Broker b) {
		this.internalBroker = b;
	}
	
	@Override
	public boolean bind(int port, QueueAcceptListener listener) {
		this.externalQueueAcceptListener = listener;
		this.internalBroker.accept(port, internalAcceptListener);
		return false;
	}

	@Override
	public boolean unbind(int port) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(String name, int port, QueueConnectListener listener) {
		this.externalQueueConnectListener = listener;
		return this.internalBroker.connect(port, name, internalConnectListener);
	}

}
