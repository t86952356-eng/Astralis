package astralis.network.websocket;

public class PingFrame extends ControlFrame {
    public PingFrame() {
        super(Opcode.PING);
    }
}
