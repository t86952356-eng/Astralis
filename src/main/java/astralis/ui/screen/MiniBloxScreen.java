package astralis.ui.screen;

import astralis.Astralis;
import astralis.network.proxy.MiniBloxMoveSync;
import astralis.ui.component.SapphireScreen;
import astralis.util.LocalProxy;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;
import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public final class MiniBloxScreen extends Screen implements InstanceAccess, SapphireScreen {
    private final Screen parentScreen;
    private boolean proxyStarted;
    private boolean connectionStarted;

    public MiniBloxScreen(Screen screen) {

        super(Component.literal("MiniBlox"));
        this.parentScreen = screen;
    }

    @Override
    protected void init() {
        Builder cancelButtonBuilder =
                Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose());
        int buttonX = this.width / 2;
        int buttonY;

        buttonX -= 100;
        buttonY = this.height / 2;

        buttonY += 42;

        this.addRenderableWidget(cancelButtonBuilder.bounds(buttonX, buttonY, 200, 20).build());
        if (!this.proxyStarted) {
            this.proxyStarted = true;
            Astralis.getInstance()
                    .getManagerRegistry()
                    .getManager(MiniBloxMoveSync.class)
                    .setEnabled(true);
            LocalProxy.restart();
        }
    }

    @Override
    public void tick() {
        if (!this.connectionStarted && LocalProxy.isReady()) {
            this.connectionStarted = true;
            ViaFabricPlus.getImpl().setTargetVersion(ProtocolVersion.v1_8, true);

            ServerData serverData = new ServerData("MiniBlox", "127.0.0.1:25565", Type.OTHER);
            ConnectScreen.startConnecting(
                    this.parentScreen,
                    this.minecraft,
                    ServerAddress.parseString(serverData.ip),
                    serverData,
                    false,
                    null);
        }
    }

    @Override
    public void onClose() {
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(MiniBloxMoveSync.class)
                .setEnabled(false);
        LocalProxy.stop();
        this.minecraft.gui.setScreen(this.parentScreen);
    }

    @Override
    public void renderSapphire(Render2DContext context) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 18.0F);

        SizedFont statusFont = FontManager.getFont("Product Sans Regular", 8.5F);
        String statusText = LocalProxy.getStatus();
        float statusWidth =
                statusFont.getMsdfFont().getStringWidth(statusText, statusFont.getSize());
        int statusColorRgb;
        if (LocalProxy.hasFailed()) {

            statusColorRgb = new Color(255, 100, 110).getRGB();
        } else {

            statusColorRgb = new Color(190, 194, 205).getRGB();
        }

        int statusColor = statusColorRgb;
        Astralis.getInstance()
                .getRenderEffects()
                .drawBlurredRoundedRectangle(
                        this.width / 2.0F - 120.0F,
                        this.height / 2.0F - 55.0F,
                        240.0F,
                        92.0F,
                        8.0F,
                        1.0F);
        RoundedRectShape panelShape =
                RoundedRectShape.obtain()
                        .setBounds(
                                this.width / 2.0F - 120.0F,
                                this.height / 2.0F - 55.0F,
                                240.0F,
                                92.0F)
                        .setRadius(8.0F);

        context.drawRect(panelShape.setColor(new Color(18, 20, 25, 185)));
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        context.drawText(
                titleRequest
                        .setText("MiniBlox")
                        .setPosition(this.width / 2.0F - 120.0F, this.height / 2.0F - 41.0F)
                        .centerHorizontally(240.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(statusText)
                        .setPosition(this.width / 2.0F - 112.0F, this.height / 2.0F - 5.0F)
                        .centerHorizontally(224.0F)
                        .setFont(statusFont.getMsdfFont())
                        .setFontSize(
                                statusWidth > 224.0F
                                        ? statusFont.getSize() * 224.0F / statusWidth
                                        : statusFont.getSize())
                        .setColor(statusColor));
    }
}
