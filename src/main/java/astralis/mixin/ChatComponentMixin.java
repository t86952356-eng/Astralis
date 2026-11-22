package astralis.mixin;

import astralis.Astralis;
import astralis.manager.ManagerRegistry;
import astralis.module.impl.Chat;
import astralis.ui.ChatScrollAccessor;
import astralis.ui.chat.ChatTabManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentMixin implements ChatScrollAccessor {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;
    @Unique private Component astralis$stackBase;
    @Unique private Component astralis$stackedContent;
    @Unique private int astralis$stackCount;
    @Unique private GuiMessage astralis$pendingStackedMessage;
    @Unique private static final int moreChatiesQwQ = 100000000;

    @Unique
    @Override
    public int astralis$getScrollPosition() {
        return this.chatScrollbarPos;
    }

    @Unique
    @Override
    public int astralis$getMaximumScroll() {
        return Math.max(
                0, this.trimmedMessages.size() - ((ChatComponent) (Object) this).getLinesPerPage());
    }

    @Unique
    @Override
    public void astralis$setScrollPosition(int scrollPosition) {
        this.chatScrollbarPos = Math.clamp(scrollPosition, 0, this.astralis$getMaximumScroll());
        if (this.chatScrollbarPos == 0) {
            this.newMessageSinceScroll = false;
        }
    }

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelBackgroundChatRender(
            GuiGraphicsExtractor guiGraphicsExtractor,
            Font font2,
            int tickCount,
            int mouseX,
            int mouseY,
            DisplayMode displayMode,
            boolean changeCursorOnInsertions,
            CallbackInfo ci) {
        Chat chat2 = Astralis.getInstance().getModuleManager().getModule(Chat.class);
        if (chat2.isEnabled()
                && chat2.getShowChatOnKeySetting().getValue()
                && !displayMode.foreground) {
            ci.cancel();
        }
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void filterAndStackMessage(GuiMessage guiMessage, CallbackInfo ci) {
        ManagerRegistry registry = Astralis.getInstance().getManagerRegistry();
        if (registry != null
                && !registry.isEmpty()
                && !registry.getManager(ChatTabManager.class).shouldShowMessage(guiMessage)) {
            ci.cancel();
        } else {
            Chat chat2 = Astralis.getInstance().getModuleManager().getModule(Chat.class);
            if (chat2 != null
                    && chat2.isEnabled()
                    && chat2.getChatStackingSetting().getValue()
                    && !this.allMessages.isEmpty()) {
                if (this.astralis$stackedContent == null
                        || !this.astralis$stackedContent.equals(guiMessage.content())) {
                    GuiMessage guiMessage2 = (GuiMessage) (Object) this.allMessages.getFirst();
                    if (this.astralis$stackBase != null
                            && this.astralis$stackBase.equals(guiMessage.content())) {
                        this.astralis$pendingStackedMessage =
                                this.createStackedMessage(guiMessage, guiMessage2);
                        ci.cancel();
                    } else {
                        this.startNewStack(guiMessage.content());
                    }
                }
            } else {
                this.startNewStack(guiMessage.content());
            }
        }
    }

    @Inject(method = "addMessageToQueue", at = @At("HEAD"), cancellable = true)
    private void applyPendingStackedMessage(GuiMessage guiMessage, CallbackInfo ci) {
        if (this.astralis$pendingStackedMessage != null && !this.allMessages.isEmpty()) {
            this.allMessages.set(0, this.astralis$pendingStackedMessage);
            this.astralis$pendingStackedMessage = null;
            ci.cancel();
        }
    }

    @Unique
    private void startNewStack(Component component) {
        this.astralis$stackBase = component;
        this.astralis$stackedContent = null;
        this.astralis$stackCount = 1;
        this.astralis$pendingStackedMessage = null;
    }

    @Unique
    private GuiMessage createStackedMessage(GuiMessage guiMessage, GuiMessage guiMessage2) {
        this.astralis$stackCount++;
        MutableComponent mutableComponent =
                guiMessage
                        .content()
                        .copy()
                        .append(
                                Component.literal(" (" + this.astralis$stackCount + "x)")
                                        .withStyle(ChatFormatting.GRAY));
        GuiMessage guiMessage3 =
                new GuiMessage(
                        guiMessage.addedTime(),
                        mutableComponent,
                        guiMessage.signature(),
                        guiMessage.source(),
                        guiMessage.tag());
        this.astralis$stackedContent = mutableComponent;
        this.trimmedMessages.removeIf(line -> line.parent() == guiMessage2);
        Minecraft minecraft2 = Minecraft.getInstance();
        int chatWidth = ChatComponent.getWidth(minecraft2.options.chatWidth().get());
        int scaledWidth = (int) Math.floor(chatWidth / minecraft2.options.chatScale().get());
        List lines = guiMessage3.splitLines(minecraft2.font, scaledWidth);
        int i = 0;

        while (i < lines.size()) {
            this.trimmedMessages.addFirst(
                    new Line(
                            guiMessage3,
                            (FormattedCharSequence) lines.get(i),
                            i == lines.size() - 1));
            i++;
        }

        return guiMessage3;
    }

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
    private int extendDisplayQueueHistoryLimit(int original) {
        Chat chat2 = Astralis.getInstance().getModuleManager().getModule(Chat.class);
        return chat2.isEnabled() && chat2.getMoreChatHistorySetting().getValue()
                ? 100000000
                : original;
    }

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int extendMessageQueueHistoryLimit(int original) {
        Chat chat2 = Astralis.getInstance().getModuleManager().getModule(Chat.class);
        return chat2.isEnabled() && chat2.getMoreChatHistorySetting().getValue()
                ? 100000000
                : original;
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void clearSocialChatHistory(boolean clearSentMessages, CallbackInfo ci) {
        if (Astralis.getInstance().getIrcChatManager() != null) {
            Astralis.getInstance().getIrcChatManager().resetDisplayState();
        }
    }
}
