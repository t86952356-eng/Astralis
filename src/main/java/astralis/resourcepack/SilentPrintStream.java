package astralis.resourcepack;

import java.io.OutputStream;
import java.io.PrintStream;

public class SilentPrintStream extends PrintStream {
    SilentPrintStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String x) {}

    @Override
    public void println(Object x) {}
}
