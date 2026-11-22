package astralis.network.websocket;

public abstract class DataFrame extends FramedataImpl1 {

    public DataFrame(Opcode opcode) {
        super(opcode);
    }

    @Override
    public void isValid() throws InvalidDataException {}
}
