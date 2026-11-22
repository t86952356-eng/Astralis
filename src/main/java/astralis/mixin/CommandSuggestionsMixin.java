package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.CommandSuggestionEvent;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private EditBox input;

    @Shadow
    private CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> pendingSuggestions;

    @Shadow private ParseResults currentParse;

    @Shadow
    public abstract void showSuggestions(boolean immediate);

    @Shadow
    public abstract void hide();

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    public void postCommandSuggestionEvent(CallbackInfo ci) {
        this.hide();
        this.input.setSuggestion(null);
        StringReader reader = new StringReader(this.input.getValue());
        ArrayList suggestionList = new ArrayList();
        CommandSuggestionEvent event =
                new CommandSuggestionEvent(reader, suggestionList, this.input.getCursorPosition());
        Astralis.getInstance().getEventBus().post(event);
        if (!event.getSuggestions().isEmpty()) {
            int cursor = this.input.getCursorPosition();
            int atIndex = reader.getString().lastIndexOf(64, Math.max(0, cursor - 1));
            int startIndex =
                    atIndex >= 0
                            ? atIndex
                            : reader.getString().lastIndexOf(32, Math.max(0, cursor - 1)) + 1;
            SuggestionsBuilder builder = new SuggestionsBuilder(reader.getString(), startIndex);
            event.getSuggestions().forEach(builder::suggest);
            this.pendingSuggestions = CompletableFuture.completedFuture(builder.build());
            this.showSuggestions(false);
            ci.cancel();
        } else {
            if (event.getDispatcher() != null && event.getSource() != null) {
                this.currentParse = event.getDispatcher().parse(reader, event.getSource());
                int cursor = this.input.getCursorPosition();
                if (cursor >= 1
                        && Minecraft.getInstance().gui.screen().getFocused() == this.input) {
                    this.pendingSuggestions =
                            event.getDispatcher()
                                    .getCompletionSuggestions(this.currentParse, cursor);
                    this.pendingSuggestions.thenAccept(
                            suggestions -> {
                                if (suggestions.isEmpty()) {
                                    this.hide();
                                } else {
                                    this.showSuggestions(false);
                                }
                            });
                }

                ci.cancel();
            }
        }
    }
}
