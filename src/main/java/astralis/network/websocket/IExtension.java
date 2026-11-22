package astralis.network.websocket;

public interface IExtension {
    @Override
    String toString();

    void decodeFrame(Framedata frame);

    String getProvidedExtensionAsClient();

    IExtension copyInstance();

    String getProvidedExtensionAsServer();

    void isFrameValid(Framedata frame) throws InvalidDataException;

    boolean acceptProvidedExtensionAsServer(String extension);

    boolean acceptProvidedExtensionAsClient(String extension);

    void encodeFrame(Framedata frame);

    void reset();
}
