package implems;

import java.util.concurrent.atomic.AtomicBoolean;
import utils.CircularBuffer;
import utils.EventPump;
import utils.EventPump.VerboseLevel;

public class Channel extends abstracts.Channel {
	
	public interface ReadListener {
	    void read(byte[] bytes);
	    void available();
	}

    public interface WriteListener {
    	void written(int byteWrote);
    }
	
	private volatile boolean localDisconnected = false;   
	private final AtomicBoolean remoteDisconnected; 

	ReadListener listener;
	
    protected CircularBuffer buffIn;
	protected CircularBuffer buffOut;

	public Channel(CircularBuffer buffIn,  CircularBuffer buffOut, AtomicBoolean disconnected) {
		this.buffIn = buffIn;
		this.buffOut = buffOut;
		this.remoteDisconnected = disconnected;
		
		EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal channel created");
	}
	
	@Override
	public void run() {
		if(!this.buffIn.empty() && this.listener != null) {
			EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal available event");
			Task.task().post(()-> this.listener.available(), "Internal available event");
		}
		EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal looping READ event"); 			
		Task.task().post(this, "Internal looping READ event");
	}
	
	
	@Override
	public boolean read(byte[] bytes, int offset, int length) {
		
		// Allow reading until input buffer is empty or local side disconnects
		if (localDisconnected) {
			return false;
		}
		
		if (buffIn.empty()) {
			if (remoteDisconnected.get()) return false;
			

			if(this.listener != null){
			 	EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal read event of size 0");
				Task.task().post(() -> this.listener.read(new byte[0]), "Internal read event of size 0");
			}
				
			
			return true;
		}
		
		int i = 0;
		while(i < length && !buffIn.empty()) {
			bytes[offset + i] = buffIn.pull();
			i++;
		} 
		
		// Create a new array with the correct size
		byte[] newBytes = new byte[i];
		System.arraycopy(bytes, offset, newBytes, 0, i);
		
		if(this.listener != null) {
			EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Internal read of size : " + newBytes.length);
			Task.task().post(() -> this.listener.read(newBytes), "Internal read of size : " + newBytes.length);
		}
		return true;
	}

	public boolean write(byte[] bytes, int offset, int length, WriteListener wl) {
		// If local or remote side is disconnected, writing is not allowed
		if (localDisconnected) {
			return false;
		}

		if (remoteDisconnected.get()) {
			// Silently ignore writing if remote side is disconnected "Wrote length"
			EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Internal DROPPED write event : " + length);
			Task.task().post(()-> wl.written(length), ("Internal DROPPED write event : " + length));
			return true; 
		}
		
		final int[] bytesWritten = {0}; // Use a final array as a wrapper

		
		if (buffOut.full()) {
			EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal write event : 0");
			Task.task().post(()-> wl.written(0), "Internal write event : 0");
			return true; // "Wrote 0"
		}
		
		while (bytesWritten[0] < (length - offset) && !buffOut.full()) {
				buffOut.push(bytes[offset + bytesWritten[0]]);
				bytesWritten[0]++;
				
		}
		
		// "Wrote bytesWritten"
		EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Internal bytes written event : " + bytesWritten[0]);
		Task.task().post(()-> wl.written(bytesWritten[0]), "Internal bytes written event : " + bytesWritten[0]);
		
		return true;
	}

	// Here we have a disconnect method that sets the localDisconnected flag to true 
	// we donc care about the local one as we wont be able to read or write anymore
	// We could have returned boolean and return a closed event, this might be a better option
	@Override
	public void disconnect() {
		EventPump.log(VerboseLevel.LOW_VERBOSE, "Internal channel disconnected");
		localDisconnected = true;
		remoteDisconnected.set(true);
	}

	// If any of the two sides is disconnected, the channel is disconnected
	@Override
	public boolean disconnected() {
		return localDisconnected || remoteDisconnected.get();
	}


	@Override
	public void setListener(ReadListener listener) {
		EventPump.log(VerboseLevel.HIGH_VERBOSE, "Internal channel listener set");
		this.listener = listener;
		// Start the reading process
		Task.task().post(this, "Initial internal READ event");
	}
}
