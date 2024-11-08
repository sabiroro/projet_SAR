package tests;

import abstracts.Channel;
import implems.Broker;
import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;
import utils.EventPump;
import implems.Task;

public class BasicTest {
	
	public static void main(String[] args) {
		Broker b = new Broker("Broker");
		
		ConnectListener cl = new ConnectListener() {
            @Override
            public void connected(Channel queue) {
            	System.out.println("Connected");
            }
			@Override
			public void refused() { System.err.println("Should not fail");}
        };
        
        AcceptListener al = new AcceptListener() {	
			@Override
			public void accepted(Channel queue) {
				System.out.println("Accepted");
			}
		};
		
		EventPump.getInstance().start();
		System.out.println("Lets go");
		Task.task().post(()-> b.accept(8080, al));
		Task.task().post(()-> b.connect(8080, "Broker", cl));
	}
}
