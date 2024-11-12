package abstracts;

public abstract class QueueBroker {

	// Constructor QueueBroker(String name);
	public abstract interface QueueAcceptListener {
		void accepted(MessageQueue queue);
	}
	public abstract boolean bind(int port, QueueAcceptListener listener);
	public abstract boolean unbind(int port);
	
	public abstract interface QueueConnectListener {
		void connected(MessageQueue queue);
		void refused();
	}
	public abstract boolean connect(String name, int port, QueueConnectListener listener);
}