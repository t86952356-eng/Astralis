package astralis.network.websocket;

import java.nio.ByteBuffer;

public interface Framedata {
    boolean getTransfereMasked();

    ByteBuffer getPayloadData();

    boolean isFin();

    void append(Framedata nextFrame);

    boolean isRSV2();

    Opcode getOpcode();

    boolean isRSV3();

    boolean isRSV1();
}
