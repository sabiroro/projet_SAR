package implems;

public class Message {
    byte[] bytes;
    int offset;
    int length;

    public Message(String message) {
    	this.bytes = message.getBytes();
    	this.offset = 0;
    	this.length = this.bytes.length;
    }
    
    public Message() {
    	
    };
    
    public Message(int size) {
    	this.bytes = new byte[size];
    	this.offset = 0;
    	this.length = size;
    };
    
    public byte[] getBytes() {
    	return bytes;
    }
    
    public void addBytes(byte[] add_bytes) {
        // Ensure there is enough capacity in the `bytes` array
        if (offset + add_bytes.length > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("Not enough space in the `bytes` array.");
        }

        // Copy the bytes from `add_bytes` starting from its beginning (0) 
        // to the `bytes` array starting at the `offset`
        System.arraycopy(add_bytes, 0, bytes, offset, add_bytes.length);
        
        // Update the length of the message to reflect the added bytes
        offset += add_bytes.length;
    }
    
    public boolean hasSize() {
    	if (bytes.length >= 4) return true;
    	return false;
    }
    
    public int getSize() {
    	return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
    }
    
    public static int getSize(byte[] bytes) {
    	return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
    }
    
    @Override
    public String toString() {
    	return new String(bytes);
    }
}
