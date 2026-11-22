package astralis.setting;

import astralis.module.Module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockListSetting extends Setting<Set<Block>> {
    private final List<Block> allBlocks;

    public BlockListSetting(String id, Module module2, Collection<Block> initialBlocks) {
        super(
                id,
                module2,
                initialBlocks == null ? new LinkedHashSet<>() : new LinkedHashSet<>(initialBlocks));
        ArrayList sortedBlocks = new ArrayList();

        for (Block block2 : BuiltInRegistries.BLOCK) {
            sortedBlocks.add(block2);
        }

        sortedBlocks.sort(Comparator.comparing(BlockListSetting::registryKey));
        this.allBlocks = List.copyOf(sortedBlocks);
    }

    public BlockListSetting(String id, Collection<Block> initialBlocks) {
        this(id, null, initialBlocks);
    }

    private static String registryKey(Block block2) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block2);
        return identifier == null ? "" : identifier.toString();
    }

    public void setValueRaw(Set<Block> newValue) {
        this.setValue(newValue);
    }

    public void setBlocks(Collection<Block> blocks) {
        this.setValue(
                blocks == null
                        ? new LinkedHashSet<>()
                        : blocks.stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public boolean isSelected(Block block2) {
        return block2 != null && this.getValue().contains(block2);
    }

    public List<Block> getAllBlocks() {
        return this.allBlocks;
    }

    public List<Block> filtered(String filter) {
        String query = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return this.allBlocks.stream()
                .filter(
                        block2 ->
                                query.isEmpty()
                                        || this.getBlockId(block2)
                                                .toLowerCase(Locale.ROOT)
                                                .contains(query)
                                        || this.getDisplayName(block2)
                                                .toLowerCase(Locale.ROOT)
                                                .contains(query))
                .sorted(
                        (block3, block4) ->
                                Boolean.compare(this.isSelected(block4), this.isSelected(block3)))
                .toList();
    }

    public Set<Block> getValue() {
        return Collections.unmodifiableSet((Set<? extends Block>) super.getValue());
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }

    public void toggleBlock(Block block2) {
        if (block2 != null) {
            LinkedHashSet blocks = new LinkedHashSet<>(this.getValue());
            if (!blocks.add(block2)) {
                blocks.remove(block2);
            }

            this.setValue(blocks);
        }
    }

    public void setValue(Set<Block> newValue) {
        super.setValue(newValue == null ? new LinkedHashSet() : new LinkedHashSet(newValue));
    }

    public String getDisplayName(Block block2) {
        Identifier identifier = block2 == null ? null : BuiltInRegistries.BLOCK.getKey(block2);
        if (identifier == null) {
            return "Unknown Block";
        }

        String path = identifier.getPath();

        String spaced = path.replace('_', ' ');
        StringBuilder builder = new StringBuilder();

        for (String word : spaced.split(" ")) {
            if (!word.isEmpty()) {
                if (!builder.isEmpty()) {

                    builder.append(' ');
                }

                builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }

        return builder.toString();
    }

    public String getBlockId(Block block2) {
        if (block2 == null) {
            return "";
        }

        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block2);
        return identifier == null ? "" : identifier.toString();
    }
}
