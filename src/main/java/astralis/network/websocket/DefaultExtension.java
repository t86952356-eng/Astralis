package astralis.network.websocket;

public class DefaultExtension implements IExtension {

    @Override
    public boolean equals(Object object) {
        return this == object || object != null && this.getClass() == object.getClass();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public void decodeFrame(Framedata frame) {}

    @Override
    public String getProvidedExtensionAsClient() {
        return "";
    }

    @Override
    public IExtension copyInstance() {
        return new DefaultExtension();
    }

    @Override
    public String getProvidedExtensionAsServer() {
        return "";
    }

    @Override
    public void isFrameValid(Framedata frame) throws InvalidDataException {
        if (!frame.isRSV1() && !frame.isRSV2() && !frame.isRSV3()) {
        } else {
            boolean rsv1 = frame.isRSV1();
            boolean rsv3 = frame.isRSV3();
            boolean rsv2 = frame.isRSV2();

            throw new InvalidFrameException(
                    "bad rsv RSV1: " + rsv1 + " RSV2: " + rsv2 + " RSV3: " + rsv3);
        }
    }

    @Override
    public boolean acceptProvidedExtensionAsServer(String extension) {
        return true;
    }

    @Override
    public boolean acceptProvidedExtensionAsClient(String extension) {
        return true;
    }

    @Override
    public void encodeFrame(Framedata frame) {}

    @Override
    public void reset() {}
}
