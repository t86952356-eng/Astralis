package astralis.event.impl;

import astralis.event.Event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;

import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;

/**
 * Posted when chat command suggestions update; handlers may supply plain suggestions or a
 * dispatcher and source for parsing.
 */
public class CommandSuggestionEvent implements Event {
    private final StringReader reader;
    private final List<String> suggestions;
    private final int cursor;
    private CommandDispatcher<SharedSuggestionProvider> dispatcher;
    private SharedSuggestionProvider source;

    public CommandSuggestionEvent(StringReader reader, List<String> suggestions, int cursor) {
        this.reader = reader;
        this.suggestions = suggestions;
        this.cursor = cursor;
    }

    public void setDispatcher(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public SharedSuggestionProvider getSource() {
        return this.source;
    }

    public CommandDispatcher<SharedSuggestionProvider> getDispatcher() {
        return this.dispatcher;
    }

    public void setSource(SharedSuggestionProvider sharedSuggestionProvider) {
        this.source = sharedSuggestionProvider;
    }

    public StringReader getReader() {
        return this.reader;
    }

    public int getCursor() {
        return this.cursor;
    }

    public List<String> getSuggestions() {
        return this.suggestions;
    }
}
