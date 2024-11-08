package abstracts;

import implems.Channel.ReadListener;
import implems.Channel.WriteListener;

public abstract class Channel {
	  public abstract void setListener(ReadListener listener);
	  public abstract boolean read(byte[] bytes, int offset, int length);
	  public abstract boolean write(byte[] bytes, int offset, int length, WriteListener listener);
	  public abstract void disconnect();
	  public abstract boolean disconnected();
}