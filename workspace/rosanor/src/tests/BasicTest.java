package tests;

import abstracts.Channel;
import implems.Broker;
import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;
import implems.Channel.ReadListener;
import implems.Channel.WriteListener;
import implems.Task;
import utils.EventPump;

public class BasicTest {
	
	public static void main(String[] args) {
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
	}
}
