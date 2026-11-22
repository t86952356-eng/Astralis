package astralis.network.websocket;

import java.util.Iterator;

public interface Handshakedata {
    boolean hasFieldValue(String name);

    Iterator<String> iterateHttpFields();

    String getFieldValue(String name);

    byte[] getContent();
}
