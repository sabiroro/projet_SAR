package implems;

import abstracts.Channel;
import implems.Channel.ReadListener;
import implems.Channel.WriteListener;
import java.nio.ByteBuffer;
import utils.EventPump;
import utils.EventPump.VerboseLevel;

public class MessageQueue extends abstracts.MessageQueue {

	protected Channel internalChannel;
	protected QueueListener externalQueueListener;
	
	protected Message currentReadMessage;
	protected Message currentSendMessage;
    
    // Sender automaton states
    enum SenderState {
        SENDING_SIZE,
        SENDING_MESSAGE
    }
    
    // Reader automaton states
    enum ReaderState {
        READING_SIZE,
        READING_MESSAGE
    }

	private SenderState senderState = SenderState.SENDING_SIZE;
    private ReaderState readerState = ReaderState.READING_SIZE;


    protected ReadListener internaReadlListener = new ReadListener() {

        byte[] sizeBuffer = new byte[4]; // Buffer for storing the size
        int sizeBufferOffset = 0;        // Offset to track bytes read into sizeBuffer

        @Override
        public void read(byte[] bytes) {
            switch (readerState) {
	            case READING_SIZE -> {
	                int bytesToCopy = Math.min(bytes.length, 4 - sizeBufferOffset);
	                System.arraycopy(bytes, 0, sizeBuffer, sizeBufferOffset, bytesToCopy);
	                sizeBufferOffset += bytesToCopy;
	
	                if (sizeBufferOffset == 4) {
	                    int size = Message.getSize(sizeBuffer);
	                    currentReadMessage = new Message(size);
	                    readerState = ReaderState.READING_MESSAGE;
	                    sizeBufferOffset = 0; // Reset offset for the next size read
	                    EventPump.log(VerboseLevel.LOW_VERBOSE, "Reader Automaton: Size determined, reading message of size " + size);
	                }
	            }
                case READING_MESSAGE -> {
                    currentReadMessage.addBytes(bytes);

                    // Check if the full message has been read
                    if (currentReadMessage.offset == currentReadMessage.length) {
                    	Task.task().post(() -> externalQueueListener.received(currentReadMessage.bytes), "External received event");
                        readerState = ReaderState.READING_SIZE;
                        EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Reader Automaton: Full message received, switching to READING_SIZE");
                    }
                }
                default -> System.err.println("Error: Unknown state in reader automaton");
            }
        }

        @Override
        public void available() {
            // Determine the number of bytes needed based on the current state
            switch (readerState) {
	            case READING_SIZE -> {
	                int remainingSizeBytes = 4 - sizeBufferOffset;
	                byte[] buffer = new byte[remainingSizeBytes];
	                internalChannel.read(buffer, 0, buffer.length);
	                EventPump.log(VerboseLevel.LOW_VERBOSE, "Reader Automaton: Requested " + buffer.length + " bytes for size read");
	            }
	            case READING_MESSAGE -> {
	                int remainingMessageBytes = currentReadMessage.length - currentReadMessage.offset;
	                byte[] buffer = new byte[remainingMessageBytes];
	                internalChannel.read(buffer, 0, buffer.length);
	                EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Reader Automaton: Requested " + buffer.length + " bytes for message read");
	            }

                default -> System.err.println("Error: Unknown state in reader automaton");
            }
        }
    };

	
	protected WriteListener internalWriteListener = new WriteListener() {

		int byteSentTotal = 0;
		
		@Override
		public void written(int byteWrote) {
			byteSentTotal += byteWrote;
			
			// This switch is a rule switch -> https://stackoverflow.com/a/77889562
			// It does not need a break statement
			switch (senderState) {
				case SENDING_SIZE -> {
				    if (byteSentTotal < 4) {
				        byte[] lengthBytes = ByteBuffer.allocate(4).putInt(currentSendMessage.length).array();
				        internalChannel.write(lengthBytes, byteWrote, 4 - byteWrote, internalWriteListener);
				        EventPump.log(VerboseLevel.LOW_VERBOSE, "Sender Automaton: Writing size bytes, progress: " + byteSentTotal + "/4");
				    } else {
				        senderState = SenderState.SENDING_MESSAGE;
				        byteSentTotal = 0;
				        internalChannel.write(currentSendMessage.bytes, byteSentTotal, currentSendMessage.length, internalWriteListener);
				        EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Sender Automaton: Size sent, switching to SENDING_MESSAGE");
				    }
				}
				case SENDING_MESSAGE -> {
				    if (byteSentTotal < currentSendMessage.length) {
				        internalChannel.write(currentSendMessage.bytes, byteSentTotal, currentSendMessage.length, internalWriteListener);
				        EventPump.log(VerboseLevel.LOW_VERBOSE, "Sender Automaton: Sending message bytes, progress: " + byteSentTotal + "/" + currentSendMessage.length);
				    } else {
				        senderState = SenderState.SENDING_SIZE;
				        byteSentTotal = 0;
				        Task.task().post(() -> externalQueueListener.sent(currentSendMessage), "External sent event");
				        EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "Sender Automaton: Message sent, switching to SENDING_SIZE");
				    }
				}
				default -> {
									System.err.println("Error : Unknown state");
						}
			}
		}
		
	};
	
	public MessageQueue(Channel channel) {
		this.internalChannel = channel;
	}
	
	@Override
	public void setListener(QueueListener l) {
        EventPump.log(VerboseLevel.MEDIUM_VERBOSE, "MessageQueue: Setting listener");
		this.externalQueueListener = l;
		// This will start the reading process, ensuring we can listen to it
		this.internalChannel.setListener(internaReadlListener);
	}

	@Override
	public boolean send(Message msg) {
		EventPump.log(VerboseLevel.HIGH_VERBOSE, "MessageQueue: Sending message");
	    
		// Buffer used to send the size of the message
	    byte[] lengthBytes = ByteBuffer.allocate(4).putInt(msg.length).array();
	    
		// Save the message to send
	    this.currentSendMessage = msg;

	    this.internalChannel.write(lengthBytes, 0, 4, internalWriteListener);
	    
	    return true;
	}


	@Override
	public void close() {
		EventPump.log(VerboseLevel.LOW_VERBOSE, "MessageQueue: Closing");
		internalChannel.disconnect();
		Task.task().post(()-> this.externalQueueListener.closed(), "MessageQueue: Closed event");
	}

	@Override
	public boolean closed() {
		return internalChannel.disconnected();
	}
	
	


}
