package abstracts;

import implems.Message;

public abstract class MessageQueue {
	public abstract interface QueueListener {
		void received(byte[] msg);
		void sent(Message msg);
		void closed();
	}
	public abstract void setListener(QueueListener l);
	public abstract boolean send(Message msg);
	public abstract void close();
	public abstract boolean closed();
}