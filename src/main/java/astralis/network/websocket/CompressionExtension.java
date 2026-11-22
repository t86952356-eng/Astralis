package astralis.network.websocket;

public abstract class CompressionExtension extends DefaultExtension {
    @Override
    public void isFrameValid(Framedata frame) throws InvalidDataException {
        if (!(frame instanceof DataFrame) || !frame.isRSV2() && !frame.isRSV3()) {
            if (frame instanceof ControlFrame
                    && (frame.isRSV1() || frame.isRSV2() || frame.isRSV3())) {
                boolean rsv1 = frame.isRSV1();
                boolean rsv3 = frame.isRSV3();
                boolean rsv2 = frame.isRSV2();

                throw new InvalidFrameException(
                        "bad rsv RSV1: " + rsv1 + " RSV2: " + rsv2 + " RSV3: " + rsv3);
            }
        } else {
            boolean rsv1 = frame.isRSV1();
            boolean rsv3 = frame.isRSV3();
            boolean rsv2 = frame.isRSV2();

            throw new InvalidFrameException(
                    "bad rsv RSV1: " + rsv1 + " RSV2: " + rsv2 + " RSV3: " + rsv3);
        }
    }
}
