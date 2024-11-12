import java.net.http.WebSocket.Listener;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import abstracts.QueueBroker.QueueConnectListener;
import implems.Broker.AcceptListener;
import implems.MessageQueue;
import implems.QueueBroker;

public class TestEvent {
	public static void main(String[] args) {
		try {
			test1();
			test2(1, 1);
			test2(10, 2);
			test3(100);
			test4();
			test5();

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
		abstracts.QueueBroker server = new QueueBroker("server");
		int connection_port = 6969;

		client.connect("server", connection_port, new QueueConnectListener() {
			@Override
			public void connected(MessageQueue queue) {
				queue.setListener(new Listener() {
					@Override
					public void received(byte[] msg) {
						System.out.println("	-> Message echoed : " + new String(msg));
						queue.close();
					}

					@Override
					public void sent(byte[] msg) {
						// Nothing there
					}

					@Override
					public void closed() {
						System.out.println("	-> Connection closed (client)");
						sm.release(); // Allows to end the test
					}
				});

				queue.send("Hello world!".getBytes());
			}

			@Override
			public void refused() {
				System.out.println("	-> Connection refused (client)");
				throw new IllegalStateException("	-> Connection refused (client)");
			}
		});

		server.bind(connection_port, new AcceptListener() {
			@Override
			public void accepted(MessageQueue queue) {
				queue.setListener(new Listener() {
					@Override
					public void received(byte[] msg) {
						queue.send(msg);
					}

					@Override
					public void sent(byte[] msg) {
						queue.close();
					}

					@Override
					public void closed() {
						server.unbind(connection_port);
						System.out.println("	-> Connection closed (server)");
						sm.release(); // Allows to end the test
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
		client.connect("server", connection_port, new QueueConnectListener() {
			@Override
			public void connected(MessageQueue queue) {
				queue.setListener(new Listener() {
					@Override
					public void received(byte[] msg) {
						System.out.println("	-> Echo message : " + new String(msg));
						queue.close();
					}

					@Override
					public void sent(byte[] msg) {
						// Nothing there
					}

					@Override
					public void closed() {
						System.out.println("	-> Connection closed");
						sm.release();
						if (sm.availablePermits() == 0)
							System.out.println(
									"WARNING : If test3 is running, a timeout is currently in progress, please wait roughtly 15 seconds to get TimeoutException of client and reconnection...");
					}
				});

				queue.send(("Hello world!" + ", port : " + connection_port).getBytes());
			}

			@Override
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
		server.bind(connection_port, new AcceptListener() {
			@Override
			public void accepted(MessageQueue queue) {
				queue.setListener(new Listener() {
					public void received(byte[] msg) {
						queue.send(msg);
					}

					public void sent(byte[] msg) {
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
		abstracts.QueueBroker server = new QueueBroker("server");

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
		abstracts.QueueBroker server = new QueueBroker("server");

		for (int i = 0; i < nbre_clients; i++) {
			int port = connection_port + i;
			abstracts.QueueBroker client = new QueueBroker("client" + i);
			echo_client(client, port, sm);
			if (i == nbre_clients - 1) {
				System.out.println("We will wait 16 seconds to simulate a client reconnection");
				Thread.currentThread();
				Thread.sleep(16000); // Create a client's reconnection because of Timeout
			}

			echo_server(server, port, true);
		}

		sm.acquire(); // Waits the end of the test
		System.out.println("Test 3 done !\n");
	}

	// Test the return statement of method connection
	public static void test4() throws Exception {
		System.out.println("Test 4 in progress...");

		abstracts.QueueBroker client = new QueueBroker("client");
		int connection_port = 6969;

		AcceptListener default_accept_listener = new AcceptListener() {

			@Override
			public void accepted(MessageQueue queue) {
				// Just for test statement, nothing there
			}
		};

		QueueConnectListener default_connect_listener = new QueueConnectListener() {

			@Override
			public void refused() {
				// Just for test statement, nothing there
			}

			@Override
			public void connected(MessageQueue queue) {
				// Just for test statement, nothing there
			}
		};

		// Initialization of server's method tests
		boolean client_connect_test = false;

		client_connect_test = client.connect("server", connection_port, default_connect_listener); // False
		if (client_connect_test)
			throw new Exception("The client tries to connect a not existing broker !");

		abstracts.QueueBroker server = new QueueBroker("server");

		client_connect_test = client.connect("server", connection_port, default_connect_listener); // True
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

		System.out.println("Test 4 done !\n");
	}

	// Try to connect a broker to himself
	public static void test5() throws Exception {
		System.out.println("Test 6 in progress...");
		Semaphore sm = new Semaphore(-1);

		abstracts.QueueBroker qb = new QueueBroker("client");
		int port = 123456789;

		qb.bind(port, new AcceptListener() {

			@Override
			public void accepted(MessageQueue queue) {
				queue.setListener(new Listener() {

					@Override
					public void sent(byte[] msg) {
						// Nothing there
					}

					@Override
					public void received(byte[] msg) {
						System.out.println("    -> Message received (from accept) : " + new String(msg));
						queue.close();
					}

					@Override
					public void closed() {
						qb.unbind(port);
						System.out.println("    -> Port unbinded");
						sm.release();
					}
				});

				queue.send("Hello me ! That's me !!!".getBytes());
			}
		});

		qb.connect("client", port, new QueueConnectListener() {

			@Override
			public void refused() {
				System.out.println("     -> Connection refused !!");
			}

			public void connected(MessageQueue queue) {
				queue.setListener(new Listener() {

					@Override
					public void sent(byte[] msg) {
						queue.close();
					}

					@Override
					public void received(byte[] msg) {
						System.out.println("    -> Message received (from connect) : " + new String(msg));
						queue.send(msg);
					}

					@Override
					public void closed() {
						System.out.println("    -> Connection closed (from connect)");
						sm.release();
					}
				});
			}

			@Override
			public void connected(abstracts.MessageQueue queue) {
				// Nothing there
			}
		});

		sm.acquire();
		System.out.println("Test 6 done !\n");
	}
}
