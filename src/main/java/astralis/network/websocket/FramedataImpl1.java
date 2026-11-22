package astralis.network.websocket;

import java.nio.ByteBuffer;

public abstract class FramedataImpl1 implements Framedata {
    private boolean fin;
    private Opcode opcode;
    private ByteBuffer unmaskedPayload;
    private boolean transferemasked;
    private boolean rsv1;
    private boolean rsv2;
    private boolean rsv3;

    public FramedataImpl1(Opcode opcode) {
        this.opcode = opcode;
        this.unmaskedPayload = ByteBufferUtils.getEmptyByteBuffer();
        this.fin = true;
        this.transferemasked = false;
        this.rsv1 = false;
        this.rsv2 = false;
        this.rsv3 = false;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object != null && this.getClass() == object.getClass()) {
            FramedataImpl1 other = (FramedataImpl1) object;
            if (this.fin != other.fin) {
                return false;
            } else if (this.transferemasked != other.transferemasked) {
                return false;
            } else if (this.rsv1 != other.rsv1) {
                return false;
            } else if (this.rsv2 != other.rsv2) {
                return false;
            } else if (this.rsv3 != other.rsv3) {
                return false;
            } else if (this.opcode != other.opcode) {
                return false;
            } else {
                return this.unmaskedPayload != null
                        ? this.unmaskedPayload.equals(other.unmaskedPayload)
                        : other.unmaskedPayload == null;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {

        String opcodeValue = String.valueOf(this.getOpcode());
        boolean finValue = this.isFin();
        int rsv1Value = this.isRSV1() ? 1 : 0;
        boolean rsv2Value = this.isRSV2();
        int rsv3Value = this.isRSV3() ? 1 : 0;
        int positionValue = this.unmaskedPayload.position();
        int remainingValue = this.unmaskedPayload.remaining();
        int sizeValue = this.unmaskedPayload.remaining();

        String payloadValue;
        if (sizeValue > 1000) {
            payloadValue = "(too big to display)";
        } else {
            payloadValue = new String(this.unmaskedPayload.array());
        }

        String payloadStr = payloadValue;
        int payloadLen = remainingValue;
        int payloadPos = positionValue;
        boolean rsv3Flag = rsv3Value != 0;
        boolean rsv2Flag = rsv2Value;
        boolean rsv1Flag = rsv1Value != 0;
        boolean finFlag = finValue;
        String opcodeString = opcodeValue;

        return "Framedata{ opcode:"
                + opcodeString
                + ", fin:"
                + finFlag
                + ", rsv1:"
                + rsv1Flag
                + ", rsv2:"
                + rsv2Flag
                + ", rsv3:"
                + rsv3Flag
                + ", payload length:[pos:"
                + payloadPos
                + ", len:"
                + payloadLen
                + "], payload:"
                + payloadStr
                + "}";
    }

    @Override
    public int hashCode() {

        int result = this.fin ? 1 : 0;

        result = 31 * result + this.opcode.hashCode();

        result = 31 * result + (this.unmaskedPayload != null ? this.unmaskedPayload.hashCode() : 0);

        result = 31 * result + (this.transferemasked ? 1 : 0);

        result = 31 * result + (this.rsv1 ? 1 : 0);

        result = 31 * result + (this.rsv2 ? 1 : 0);

        return 31 * result + (this.rsv3 ? 1 : 0);
    }

    public void setRSV3(boolean rsv3) {
        this.rsv3 = rsv3;
    }

    public static FramedataImpl1 get(Opcode opcode) {
        if (opcode == null) {

            throw new IllegalArgumentException("Supplied opcode cannot be null");
        }

        switch (OpcodeSwitchMap.opcodeCases[opcode.ordinal()]) {
            case 1:
                return new PingFrame();
            case 2:
                return new PongFrame();
            case 3:
                return new TextFrame();
            case 4:
                return new BinaryFrame();
            case 5:
                return new CloseFrame();
            case 6:
                return new ContinuousFrame();
            default:
                throw new IllegalArgumentException("Supplied opcode is invalid");
        }
    }

    public void setTransferemasked(boolean transferemasked) {
        this.transferemasked = transferemasked;
    }

    public void setRSV1(boolean rsv1) {
        this.rsv1 = rsv1;
    }

    @Override
    public boolean getTransfereMasked() {
        return this.transferemasked;
    }

    @Override
    public ByteBuffer getPayloadData() {
        return this.unmaskedPayload;
    }

    @Override
    public boolean isFin() {
        return this.fin;
    }

    public void setRSV2(boolean rsv2) {
        this.rsv2 = rsv2;
    }

    public void setPayload(ByteBuffer payload) {
        this.unmaskedPayload = payload;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    @Override
    public void append(Framedata nextFrame) {
        ByteBuffer nextPayload = nextFrame.getPayloadData();
        if (this.unmaskedPayload == null) {
            this.unmaskedPayload = ByteBuffer.allocate(nextPayload.remaining());
            nextPayload.mark();
            this.unmaskedPayload.put(nextPayload);
            nextPayload.reset();
        } else {
            nextPayload.mark();
            this.unmaskedPayload.position(this.unmaskedPayload.limit());
            this.unmaskedPayload.limit(this.unmaskedPayload.capacity());
            if (nextPayload.remaining() > this.unmaskedPayload.remaining()) {
                ByteBuffer combined =
                        ByteBuffer.allocate(
                                nextPayload.remaining() + this.unmaskedPayload.capacity());
                this.unmaskedPayload.flip();
                combined.put(this.unmaskedPayload);
                combined.put(nextPayload);
                this.unmaskedPayload = combined;
            } else {
                this.unmaskedPayload.put(nextPayload);
            }

            this.unmaskedPayload.rewind();
            nextPayload.reset();
        }

        this.fin = nextFrame.isFin();
    }

    @Override
    public boolean isRSV2() {
        return this.rsv2;
    }

    @Override
    public Opcode getOpcode() {
        return this.opcode;
    }

    public abstract void isValid() throws InvalidDataException;

    @Override
    public boolean isRSV3() {
        return this.rsv3;
    }

    @Override
    public boolean isRSV1() {
        return this.rsv1;
    }
}
