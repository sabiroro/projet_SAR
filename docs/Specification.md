# SAR - SPECIFICATION & DESIGN

## Aim 

The aim of this **full event** version of the broker and channel is to remove any threads. Will only remain one event pump running in one thread that handles everything.

## Broker

A **broker** is an object that allows connections to another broker (eventually itself). When 2 brokers are connected, they can send bytes between them via a **channel** which is actually a stream. A broker has for that several ports (uniques and distincts) that allow it to connect to other brokers. A port allows one connection each time (entering or exiting). Both brokers are connected on the same port number.

### Abstract methods

#### Broker

```java  
Broker(String name) {};  
abstract boolean accept(int port, AcceptListener acl);   
abstract boolean connect(int port, String name, ConnectListener cnl);   
```

#### Accept Listener

```java  
public interface AcceptListener {  
	void accepted(ChannelAbstract channel);  
}  
```

#### Connect listener

```java  
public interface ConnectListener {  
	void refused();  
	void connected(ChannelAbstract channel);  
}  
```

### **Broker Specification**

#### **Connection Setup and Listener Configuration**

* To listen for incoming connections, the broker calls the `boolean accept(int port, AcceptListener listener)` method. This returns `true` if the connection is possible and opens the specified **port** for listening. It returns `false` if a connection is not possible, such as when the port is already occupied by another `accept` or `connect` operation. The `AcceptListener` defines events for the accepting broker, with `void accepted(Channel channel)` triggered when a connection is successfully established.  
* To initiate a connection, the broker calls `boolean connect(String name, int port, ConnectListener listener)`. This method returns `true` if the connection attempt is valid, proceeding to connect, or `false` if it fails (e.g., if the port is in use or if the target broker does not exist). `ConnectListener` defines connection events, including:  
  * `void connected(Channel channel)`: Triggered when the connection is successfully established.  
  * `void refused()`: Triggered when a connection attempt is denied, for instance, if a timeout threshold (X seconds) is exceeded.

#### **Event Handling During Connection Lifecycle**

When either `accept()` or `connect()` is called, specific events (from `ConnectListener` or `AcceptListener`) are generated to signal a successful connection to the calling entities. These events provide the newly created communication channel to both participants, enabling data exchange. Notably, `connect()` and `accept()` may be invoked in any order without affecting the connection process.

#### **Port Listening Constraints and Connection Capacity**

The `accept()` method opens a listening port on the designated broker (e.g., Broker A), allowing it to handle one incoming connection request at a time.
> Note: Implementing a `bind` method would enable a port to accept multiple connections simultaneously, requiring multiple `accept()` calls.

#### **Connection Conditions and Timeout Management**

A connection channel successfully opens only if both `accept()` on Broker A and `connect()` from the same or another broker target the same port on Broker A. If `connect()` is called and:

* **The Target Broker Does Not Exist**: The `connect()` method returns `false`, signifying a refused connection.  
* **The Target Broker is Unavailable or Delayed**: If the broker exists but cannot accept immediately, an X-second timer begins. If a connection isn't established within this window, a `TimeoutException` is thrown, triggering a `refused()` signal to inform the calling process of the unavailability.

## Channel

A channel is the object that allows doing bytes circulation between 2 brokers. This channel is created when both brokers are connected successfully. The channel’s instance is different between both brokers, but they are linked to communicate. A channel can also read or send bytes.

#### Channel Lifecycle and Communication

1. **Channel Creation:**    
   A channel instance is created when two brokers successfully connect. Although each broker has a separate channel instance, these instances are linked for communication.

2. **Data Transmission:**    
   A channel allows the transmission of data through **read** and **write** operations, where each **channel** can either send or receive byte sequences.

---

### Read Operation

The **read** function allows a broker to read incoming byte sequences from the connected broker.

- **Method:** `void read(byte[] bytes, int offset, int length, ReadListener listener)`  
- **Description:** This method triggers the `read(byte[] bytes)` event whenever data is available to read.   
   - **bytes:** Buffer into which the read bytes are stored.  
   - **offset:** The starting point in the buffer for the next read operation.  
   - **length:** The number of bytes available for reading.  
   - **listener:** The listener to be notified some bytes were read.

- **Disconnection Scenario:**    
  If the remote channel disconnects, the local channel can complete reading any remaining bytes and then raises a `DisconnectException`. After handling this exception, the channel disconnects to indicate the end of communication.

---

### Write Operation

The **write** function enables a broker to send byte sequences to the connected broker.

- **Method:** `void write(byte[] bytes, int offset, int length, WriteListener listener)`  
- **Description:** This method triggers the `written(nbytes)` event once bytes have been successfully written.  
   - **bytes:** Buffer containing the bytes to be written.  
   - **offset:** The starting point in the buffer for the write operation.  
   - **length:** The number of bytes to write.  
   - **listener:** The listener to be notified when some bytes were written.
  
>  - **nbytes:** The number of bytes successfully written during this operation

- **Disconnection Scenario:**    
  If the remote channel disconnects, any data written after this point will be silently dropped, and no further write operations will be acknowledged by the remote broker.

---

### Disconnection

To terminate the communication link, either broker can initiate a **disconnect**.

- **Method:** `void disconnect()`  
- **Description:** This method immediately closes the channel.

#### Additional Listener Interface

For streamlined event handling, listeners for `read`, `available`, and `written` events can be registered:

```java  
Listener {  
    void read(byte[] bytes);  
    void available();  
    void written(int nbytes);  
}  
```

#### Channel Class Example

```java  
Channel {  
    void setListener(ReadListener listener);  
    void read(byte[] bytes, int offset, int length, ReadListener listener);  
    void write(byte[] bytes, int offset, int length, WriteListener listener);  
}  
```

## Using example

Here is an example to use these channels and brokers to create an echo server.  
```java  
Broker b = new Broker("Broker");
		
WriteListener wr = new WriteListener() {
	
	@Override
	public void written(int byteWrote) {
		System.out.println("Written");
	}
};

ConnectListener cl = new ConnectListener() {
	@Override
	public void connected(Channel queue) {
		
		ReadListener rl = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				System.out.println("Read");
			}

			@Override
			public void available() {
				// TODO Auto-generated method stub
				System.out.println("Available");
			}
		};
		
		System.out.println("Connected");
		queue.setListener(rl);
		byte[] msg = "Hello".getBytes();
		queue.write(msg, 0, msg.length, wr);
	}
	@Override
	public void refused() { System.err.println("Should not fail");}
};

AcceptListener al = new AcceptListener() {	
	@Override
	public void accepted(Channel queue, int port) {
		System.out.println("Accepted");
		
		
		ReadListener rl = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				System.out.println("Read : " + new String(bytes));
				
			}

			@Override
			public void available() {
				// TODO Auto-generated method stub
				System.out.println("Available");
				byte[] msg = new byte["Hello".getBytes().length];
				queue.read(msg, 0, "Hello".getBytes().length);
			}
		};
		
		queue.setListener(rl);
	}
};

EventPump.getInstance().start();
System.out.println("Lets go");
Task.task().post(()-> b.accept(8080, al), "Accepting");
Task.task().post(()-> b.connect(8080, "Broker", cl), "Connecting"); 
````