package astralis.util.render;

import org.lwjgl.opengl.GL30;

import java.util.Stack;

public class GlStateStack {
    private static Stack<GlStateSnapshot> snapshotStack;

    public static void pushState() {
        if (Render2DContext.isOpenGlBackend()) {
            snapshotStack.push(new GlStateSnapshot(getGlVersionCode()).capture());
        }
    }

    public static void popState() {
        if (!Render2DContext.isOpenGlBackend()) {
            return;
        }

        Object snapshot = snapshotStack;
        if (snapshotStack.empty()) {
            return;
        }

        snapshot = snapshotStack.pop();
        ((GlStateSnapshot) snapshot).restore();
    }

    public static int getGlVersionCode() {
        int[] majorVersion = new int[1];
        int[] minorVersion = new int[1];
        GL30.glGetIntegerv(33307, majorVersion);
        GL30.glGetIntegerv(33308, minorVersion);
        return majorVersion[0] * 100 + minorVersion[0] * 10;
    }

    static {
        snapshotStack = new Stack<>();
    }
}
