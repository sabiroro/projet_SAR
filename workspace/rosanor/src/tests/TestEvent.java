package tests;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import abstracts.MessageQueue.QueueListener;
import abstracts.QueueBroker.QueueAcceptListener;
import abstracts.QueueBroker.QueueConnectListener;
import implems.Message;
import implems.QueueBroker;
import utils.BrokerManager;
import utils.EventPump;

public class TestEvent {
	public static void main(String[] args) {
		try {
			
			EventPump.getInstance().start();
			test1();
			BrokerManager.getInstance().removeAllBrokers();
			EventPump.getInstance().refreshPump();

			test2(1, 1);
			BrokerManager.getInstance().removeAllBrokers();
			
			EventPump.getInstance().refreshPump();
			
			
			test2(10, 2);
			BrokerManager.getInstance().removeAllBrokers();
			EventPump.getInstance().refreshPump();
			
			test3(100);
			BrokerManager.getInstance().removeAllBrokers();
			EventPump.getInstance().refreshPump();
			
			test4();
			BrokerManager.getInstance().removeAllBrokers();
			EventPump.getInstance().refreshPump();
			
			test5();
			BrokerManager.getInstance().removeAllBrokers();
			EventPump.getInstance().refreshPump();
			
			System.out.println("That's all folks");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("	The test has failed : " + e.getMessage());
			System.exit(-1);
		}
	}

	// Echo server (from the specification doc)
	public static void test1() throws Exception {
		System.out.println("Test 1 in progress...");
		Semaphore sm = new Semaphore(0); // Allows to block the execution until the echo message

		abstracts.QueueBroker client = new QueueBroker("client");
		abstracts.QueueBroker server = new QueueBroker("server1");
		int connection_port = 6969;

		client.connect("server1", connection_port, new QueueConnectListener() {
			
			public void connected(abstracts.MessageQueue queue) {
				queue.setListener(new QueueListener() {
					
					public void received(byte[] msg) {
						System.out.println("	-> Message echoed : " + new String(msg));
						queue.close();
					}

					
					public void sent(Message msg) {
						// Nothing there
					}

					
					public void closed() {
						System.out.println("	-> Connection closed (client)");
						sm.release(); // Allows to end the test
					}
				});

				queue.send(new Message(new String("Hello world!".getBytes())));
			}

			
			public void refused() {
				System.out.println("	-> Connection refused (client)");
				throw new IllegalStateException("	-> Connection refused (client)");
			}
		});

		server.bind(connection_port, new QueueAcceptListener() {
			
			public void accepted(abstracts.MessageQueue queue) {
				queue.setListener(new QueueListener() {
					
					public void received(byte[] msg) {
						queue.send(new Message(new String(msg)));
					}

					
					public void sent(Message msg) {
						queue.close();
					}

					
					public void closed() {
						System.out.println("	-> Connection closed (server)");
						sm.release(); // Allows to end the test
						server.unbind(connection_port);
					}
				});
			}
		});

		sm.acquire(2); // Waits the end of the test
		System.out.println("Test 1 done !\n");
	}

	private static void echo_client(abstracts.QueueBroker client, int connection_port, Semaphore sm)
			throws TimeoutException {
		echo_client(client, connection_port, sm, true);
	}

	private static void echo_client(abstracts.QueueBroker client, int connection_port, Semaphore sm,
			boolean try_to_reconnect) throws TimeoutException {
		client.connect("server2", connection_port, new QueueConnectListener() {
			
			public void connected(abstracts.MessageQueue queue) {
				queue.setListener(new QueueListener() {
					
					public void received(byte[] msg) {
						System.out.println("	-> Echo message : " + new String(msg));
						queue.close();
					}

					
					public void sent(Message msg) {
						// Nothing there
					}

					
					public void closed() {
						System.out.println("	-> Connection closed");
						sm.release();
						if (sm.availablePermits() == 0)
							System.out.println(
									"WARNING : If test3 is running, a timeout is currently in progress, please wait roughtly 15 seconds to get TimeoutException of client and reconnection...");
					}
				});

				queue.send(new Message(new String(("Hello world!" + ", port : " + connection_port).getBytes())));
			}

			
			public void refused() {
				System.out.println("	-> Connection refused...");
				if (try_to_reconnect) {
					try {
						System.out.println("	-> New connecting try...");
						System.out.println("	-> Try to reconnect...");
						echo_client(client, connection_port, sm, false);
					} catch (TimeoutException e) {
						// Nothing there
					}
				}
			}
		});

	}

	/**
	 * Create an echo serv with an accepted port opened until the unbind
	 * 
	 * @param server
	 * @param connection_port
	 * @throws DisconnectedException
	 */
	private static void echo_server(abstracts.QueueBroker server, int connection_port) {
		echo_server(server, connection_port, false);
	}

	private static void echo_server(abstracts.QueueBroker server, int connection_port, boolean need_to_unbind) {
		server.bind(connection_port, new QueueAcceptListener() {
			
			public void accepted(abstracts.MessageQueue queue) {
				queue.setListener(new QueueListener() {
					public void received(byte[] msg) {
						queue.send(new Message(new String(msg)));
					}

					public void sent(Message msg) {
						if (need_to_unbind)
							queue.close();
					}

					public void closed() {
						server.unbind(connection_port);
						// System.out.println(" ---> " + connection_port + " closed from server");
					}
				});
			}
		});
	}

	// Echo server with several clients on same port
	public static void test2(int nbre_clients, int test_number) throws Exception {
		System.out.println("Test 2." + test_number + " in progress...");
		Semaphore sm = new Semaphore(1 - nbre_clients); // Allows to block the execution until the echo message

		int connection_port = 6969;
		abstracts.QueueBroker server = new QueueBroker("server2");

		for (int i = 0; i < nbre_clients; i++) {
			abstracts.QueueBroker client = new QueueBroker("client" + i);
			echo_client(client, connection_port, sm);
		}
		echo_server(server, connection_port);

		sm.acquire(); // Waits the end of the test
		System.out.println("Test 2." + test_number + " done !\n");
	}

	// Echo server with several clients on different ports
	public static void test3(int nbre_clients) throws Exception {
		System.out.println("Test 3 in progress...");
		Semaphore sm = new Semaphore(1 - nbre_clients); // Allows to block the execution until the echo message

		int connection_port = 6969;
		abstracts.QueueBroker server = new QueueBroker("server2");

		for (int i = 0; i < nbre_clients; i++) {
			int port = connection_port + i;
			abstracts.QueueBroker client = new QueueBroker("client" + i);
			echo_client(client, port, sm);
			if (i == nbre_clients - 1) {
				System.out.println("We will wait 16 seconds to simulate a client reconnection");
				Thread.currentThread();
				Thread.sleep(1000); // Create a client's reconnection because of Timeout
			}

			echo_server(server, port, true);
		}

		sm.acquire(); // Waits the end of the test
		System.out.println("Test 3 done !\n");
	}

	// Test the return statement of method connection
	public static void test4() throws Exception {
		System.out.println("Test 4 in progress...");
		Semaphore sm = new Semaphore(-1);
		
		abstracts.QueueBroker client = new QueueBroker("client");
		int connection_port = 6969;

		QueueAcceptListener default_accept_listener = new QueueAcceptListener() {

			
			public void accepted(abstracts.MessageQueue queue) {
				// Just for test statement, nothing there
			}
		};

		QueueConnectListener default_connect_listener = new QueueConnectListener() {

			
			public void refused() {
				// Just for test statement, nothing there
			}

			
			public void connected(abstracts.MessageQueue queue) {
				// Just for test statement, nothing there
			}
		};

		// Initialization of server's method tests
		boolean client_connect_test = false;

		client_connect_test = client.connect("server3", connection_port, default_connect_listener); // False
		if (client_connect_test)
			throw new Exception("The client tries to connect a not existing broker !");

		abstracts.QueueBroker server = new QueueBroker("server3");

		client_connect_test = client.connect("server3", connection_port, default_connect_listener); // True
		if (!client_connect_test)
			throw new Exception("The client doesn't find the broker !");

		// Initialization of client's method tests
		boolean server_bind_test = false;
		boolean server_unbind_test = false;

		server_unbind_test = server.unbind(connection_port); // False
		if (server_unbind_test)
			throw new Exception("The server tries to unbind a not connected port !");

		server_bind_test = server.bind(connection_port, default_accept_listener); // True
		if (!server_bind_test)
			throw new Exception("The server can't bind a connection port !");

		server_bind_test = server.bind(connection_port, default_accept_listener); // False
		if (server_bind_test)
			throw new Exception("The server tries to bind an existing port !");

		server_unbind_test = server.unbind(connection_port); // True
		if (!server_unbind_test)
			throw new Exception("The server can't unbind a connected port !");

		server_bind_test = server.bind(connection_port, default_accept_listener); // True
		if (!server_bind_test)
			throw new Exception("The server can't bind an old connection port !");
		
		if (server.unbind(connection_port) != true)
			throw new Exception("The server should unbind here");
		
		
		
		System.out.println("Test 4 done !\n");
	}

	// Try to connect a broker to himself
	public static void test5() throws Exception {
		System.out.println("Test 5 in progress...");
		int nb = 10000;
		Semaphore sm = new Semaphore(-10000);

		abstracts.QueueBroker qb = new QueueBroker("oneClient");
		int port = 6970;
		

		QueueAcceptListener qal = new QueueAcceptListener() {

			
			public void accepted(abstracts.MessageQueue queue) {
				QueueListener ql = new QueueListener() {

					
					public void sent(Message msg) {
						// Nothing there
					}

					
					public void received(byte[] msg) {
						System.out.println("    -> Message received (from accept) : " + msg);
						queue.close();
					}

					
					public void closed() {
						qb.unbind(port);
						System.out.println("    -> Port unbinded");
						sm.release();
					}
				};
				
				queue.setListener(ql);

				queue.send(new Message(new String("Hello me ! That's me !!!".getBytes())));
			}
		};
		
	

		QueueConnectListener qcl = new QueueConnectListener() {
			public void refused() {
				System.out.println("     -> Connection refused !!");
			}
			
			public void connected(abstracts.MessageQueue queue) {
				QueueListener ql = new QueueListener() {
					public void sent(Message msg) {
						queue.close();
					}

					
					public void received(byte[] msg) {
						System.out.println("    -> Message received (from connect) : " + new String(msg));
						queue.send(new Message(new String(msg)));
					}

					
					public void closed() {
						System.out.println("    -> Connection closed (from connect)");
						sm.release();
					}
				};
				queue.setListener(ql);
			}
		};
		
		for (int i = 0 ; i < nb ; i++) {
			qb.bind(port+i, qal);
			qb.connect("oneClient", port+i, qcl);
		}
		
		sm.acquire();
		System.out.println("Test 5 done !\n");
	}
}
