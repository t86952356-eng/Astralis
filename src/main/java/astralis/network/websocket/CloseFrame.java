package astralis.network.websocket;

import java.nio.ByteBuffer;

public class CloseFrame extends ControlFrame {
    public static final int NEVER_CONNECTED = -1;
    private int code;
    private String reason;

    public CloseFrame() {
        super(Opcode.CLOSING);
        this.setReason("");

        this.setCode(1000);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || this.getClass() != object.getClass()) {
            return false;
        } else if (!super.equals(object)) {
            return false;
        } else {
            CloseFrame other = (CloseFrame) object;
            if (this.code != other.code) {
                return false;
            } else {
                return this.reason != null
                        ? this.reason.equals(other.reason)
                        : other.reason == null;
            }
        }
    }

    @Override
    public String toString() {
        String base = super.toString();

        return base + "code: " + this.code;
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();

        result = 31 * result + this.code;

        return 31 * result + (this.reason != null ? this.reason.hashCode() : 0);
    }

    private void updatePayload() {
        byte[] reasonBytes = Charsetfunctions.utf8Bytes(this.reason);
        ByteBuffer codeBuffer = ByteBuffer.allocate(4);
        codeBuffer.putInt(this.code);
        codeBuffer.position(2);
        ByteBuffer payload = ByteBuffer.allocate(2 + reasonBytes.length);
        payload.put(codeBuffer);
        payload.put(reasonBytes);
        payload.rewind();
        super.setPayload(payload);
    }

    private void validateUtf8(ByteBuffer buffer, int originalPosition) throws InvalidDataException {

        try {
            buffer.position(buffer.position() + 2);
            this.reason = Charsetfunctions.stringUtf8(buffer);
        } catch (IllegalArgumentException ignored) {

            throw new InvalidDataException(1007);
        } finally {
            buffer.position(originalPosition);
        }
    }

    @Override
    public ByteBuffer getPayloadData() {

        return this.code == 1005 ? ByteBufferUtils.getEmptyByteBuffer() : super.getPayloadData();
    }

    public String getMessage() {
        return this.reason;
    }

    public void setCode(int code) {
        this.code = code;

        if (code == 1015) {

            this.code = 1005;
            this.reason = "";
        }

        this.updatePayload();
    }

    @Override
    public void setPayload(ByteBuffer payload) {

        this.code = 1005;
        this.reason = "";
        payload.mark();
        if (payload.remaining() == 0) {

            this.code = 1000;
        } else if (payload.remaining() == 1) {

            this.code = 1002;
        } else {
            if (payload.remaining() >= 2) {
                ByteBuffer codeBuffer = ByteBuffer.allocate(4);
                codeBuffer.position(2);
                codeBuffer.putShort(payload.getShort());
                codeBuffer.position(0);
                this.code = codeBuffer.getInt();
            }

            payload.reset();

            try {
                int originalPosition = payload.position();
                this.validateUtf8(payload, originalPosition);
            } catch (InvalidDataException ignored) {

                this.code = 1007;
                this.reason = null;
            }
        }
    }

    @Override
    public void isValid() throws InvalidFrameException, InvalidDataException {
        super.isValid();

        if (this.code == 1007 && this.reason.isEmpty()) {

            throw new InvalidDataException(1007, "Received text is no valid utf8 string!");
        }

        if (this.code == 1005 && 0 < this.reason.length()) {

            throw new InvalidDataException(
                    1002, "A close frame must have a closecode if it has a reason");
        }

        if (this.code > 1015) {

            if (this.code < 3000) {

                throw new InvalidDataException(1002, "Trying to send an illegal close code!");
            }
        }

        if (this.code != 1006) {

            if (this.code != 1015) {

                if (this.code != 1005) {

                    if (this.code <= 4999) {

                        if (this.code >= 1000) {

                            if (this.code != 1004) {
                                return;
                            }
                        }
                    }
                }
            }
        }

        throw new InvalidFrameException("closecode must not be sent over the wire: " + this.code);
    }

    public int getCloseCode() {
        return this.code;
    }

    public void setReason(String reason) {
        if (reason == null) {
            reason = "";
        }

        this.reason = reason;
        this.updatePayload();
    }
}
