package abstracts;

import implems.Broker.AcceptListener;
import implems.Broker.ConnectListener;

public abstract class Broker {
//	public Broker(String name) { };
	public abstract boolean accept(int port, AcceptListener acl);
	public abstract boolean connect(int port, String name, ConnectListener cnl);
}