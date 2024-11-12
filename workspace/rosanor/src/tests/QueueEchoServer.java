package tests;

import abstracts.MessageQueue;
import abstracts.MessageQueue.QueueListener;
import abstracts.QueueBroker.QueueAcceptListener;
import abstracts.QueueBroker.QueueConnectListener;
import implems.Broker;
import implems.Message;
import implems.QueueBroker;
import implems.Task;
import utils.EventPump;

public class QueueEchoServer {
	public static void main(String[] args) {
		EventPump.getInstance().start();
		
		Broker b = new Broker("Broker");
		
		QueueBroker qb = new QueueBroker(b);
		
		QueueAcceptListener qal = new QueueAcceptListener() {

			@Override
			public void accepted(MessageQueue queue) {
				System.out.println("Accepted");
				QueueListener ql = new QueueListener() {
					int msgs = 0;
					@Override
					public void sent(Message msg) {
						System.out.println("Message sent : " + new String(msg.getBytes()));
					}
					
					@Override
					public void received(byte[] msg) {
						System.out.println("Received : " + new String(msg));
						String aa =  "" + (msgs++);
						queue.send(new Message(new String(aa)));
					}
					
					@Override
					public void closed() {
						System.out.println("Closed");
					}
				};
				
				queue.setListener(ql);
			}
		};
		
		QueueConnectListener qcl = new QueueConnectListener() {

			@Override
			public void connected(MessageQueue queue) {
				System.out.println("Connected");
				System.out.println("Sending message");
				QueueListener ql = new QueueListener() {
					int msgs = 0;
					
					@Override
					public void sent(Message msg) {
						System.out.println("Message sent : " + msg);
					}
					
					@Override
					public void received(byte[] msg) {
						System.out.println("Received : " + new String(msg));
						String aa =  "" + (msgs++);
						queue.send(new Message(aa));
					}
					
					@Override
					public void closed() {
						System.out.println("Closed");
					}
				};
				
				queue.setListener(ql);
				queue.send(new Message(1));
			}

			@Override
			public void refused() {
				System.out.println("Refused");
			}
			
		};
		
		Task.task().post(() -> qb.bind(8080, qal), "Initial bind request");
		Task.task().post(() -> qb.connect("Broker", 8080, qcl), "Initial connect request");
	}
}
