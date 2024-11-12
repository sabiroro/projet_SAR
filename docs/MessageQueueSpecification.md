# Specification: QueueBroker and MessageQueue full-event

## Aim

The aim of this part of the project is to add a new layer onto channels and brokers to get a full-event communication system.    
This part will explain the use of queuebrokers and messagequeues which allows to get a message layer based on events.

## QueueBroker

A **queuebroker** is an object that allows to create a connection (entering or exiting) with another queuebroker (eventually itself). A queuebroker has a unique name which allows it to be called by other queuebrokers. A queuebroker has several ports but each one is unique and allows one connection at each time. It can **bind, unbind or connect** thanks to a port.

### Accept method

The queuebroker allows to *bind,* to open a port, that means that it waits a extern connection which is reported thanks to an event of an **AcceptListener**. This method returns a boolean to indicate if the bind is possible (e.g. free port), or not (e.g. port already binded). The bind is so not blocking and the *accepted()* event is raised when a connection is created.    
The queuebroker can so **unbind** a port to stop connection acceptance. The method returns a boolean which means if the port is unbindable (accepting port) or not (port not binded). When the *unbind* is carried out, the server connection is interrupted.

### Connect method

The queuebroker can connect another queuebroker by knowing its name and its connection port. The **ConnectListener** allows to indicate the event that took place.  
The connect is then non-blocking blocking and returns true if it accepts to connect to the speciﬁed queuebroker or false if it doesn't (non-existent name).  
The *connected* event is then pushed as soon as the connection has taken place. Otherwise, the *refused* event is raised. Moreover, if the local queuebroker connection time exceeds 15 seconds, this event is raised too.  
A broker is authorised to connect to itself.

## MessageQueue

The **messagequeue** is an object that allows to send or receive a full message determined by the local/remote messagequeue.  
> NB: A message to be sent can't exceed 2.1Go (roughly egal: 2^31-1). It needs at least 2 segmentations by the user in different queues.

### Send method

A messagequeue can send a message with the method *send* that returns a boolean to indicate if the sending is possible or not (e.g. connection lost). The message is a bytes array where we can precise the bounds [offsett, offset+length].  
This array can be modified by the user after calling this method (the array is copied by the method before the event is posted, so an ulterior modification of the array is possible).  
When the message has been sent, the event *sent* is rosen to the sender.

### Receive method

A messagequeue can read a message. When it receives all a message sent by the sender, the event *received* is rosen to the receiver.

### Close & Closed methods

A messagequeue can close (locally) its connection thanks to the method *close*. The event *closed* is raised to the sender when the connection is closed.  
The method *closed* allows to know if the connection is closed or not.

### Use and setup listeners

A messagequeue isn't instantiated by the user but by the queuebroker during the connection. The user can setup the listeners thanks to the method *setListener* that takes a *Listener* object as parameter. This object has so to implement the methods *sent* and *received* to be able to use the outcome of the methods.

## Extended example

Here is an example with an echo server :

```java  
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
````
