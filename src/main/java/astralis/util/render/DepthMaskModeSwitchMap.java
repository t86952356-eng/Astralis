package astralis.util.render;

import astralis.network.websocket.ChannelInterest;

public class DepthMaskModeSwitchMap {
    static final int[] ORDINAL_TABLE = new int[ChannelInterest.values().length];

    static {
        try {
            ORDINAL_TABLE[ChannelInterest.WRITE.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            ORDINAL_TABLE[ChannelInterest.READ.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
