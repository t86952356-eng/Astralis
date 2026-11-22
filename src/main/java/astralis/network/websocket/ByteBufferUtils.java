package astralis.network.websocket;

import java.nio.ByteBuffer;

public class ByteBufferUtils {
    private ByteBufferUtils() {}

    public static ByteBuffer getEmptyByteBuffer() {
        return ByteBuffer.allocate(0);
    }

    public static int transferByteBuffer(ByteBuffer source, ByteBuffer destination) {
        if (source != null && destination != null) {
            int sourceRemaining = source.remaining();
            int destinationRemaining = destination.remaining();
            if (sourceRemaining > destinationRemaining) {
                int count = Math.min(sourceRemaining, destinationRemaining);
                source.limit(count);
                destination.put(source);
                return count;
            } else {
                destination.put(source);
                return sourceRemaining;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
