package astralis.network.websocket;

public interface HandshakeBuilder extends Handshakedata {
    void put(String name, String value);

    void setContent(byte[] content);
}
