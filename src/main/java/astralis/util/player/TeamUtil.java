package astralis.util.player;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class TeamUtil implements InstanceAccess {
    private static int maxColorDistanceSq = 2500;
    private static EquipmentSlot[] armorSlots = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static DyeColorMapping[] dyeColorMappings = {
        new DyeColorMapping(16711680, DyeColor.RED),
        new DyeColorMapping(255, DyeColor.BLUE),
        new DyeColorMapping(32768, DyeColor.LIME),
        new DyeColorMapping(4770840, DyeColor.LIME),
        new DyeColorMapping(16776960, DyeColor.YELLOW),
        new DyeColorMapping(65535, DyeColor.CYAN),
        new DyeColorMapping(16777215, DyeColor.WHITE),
        new DyeColorMapping(8388736, DyeColor.PINK),
        new DyeColorMapping(8421504, DyeColor.GRAY)
    };
    private static Pattern colorCodePattern = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static boolean isTeammate(LivingEntity livingEntity) {
        LocalPlayer localPlayer = mc.player;
        if (mc.player.isAlliedTo(livingEntity)) {
            return true;
        }

        localPlayer = mc.player;

        Component component2 = localPlayer.getDisplayName();
        Component component3 = livingEntity.getDisplayName();
        boolean sameColor = hasSameNameColor(component2, component3);
        if (!sameColor) {
            boolean samePrefix = hasSameNamePrefix(component3, component2);
            if (!samePrefix) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasSameNameColor(Component component2, Component component3) {
        Style style = component3.getStyle();
        Style style2 = component2.getStyle();
        TextColor textColor2 = style.getColor();
        if (textColor2 == null) {
            return false;
        }

        textColor2 = style2.getColor();

        if (textColor2 == null) {
            return false;
        }

        textColor2 = style.getColor();

        return textColor2.equals(style2.getColor());
    }

    private static boolean hasSameNamePrefix(Component component2, Component component3) {
        String firstText = stripColorCodes(component2.getString());
        String secondText = stripColorCodes(component3.getString());
        String[] firstWords = firstText.split(" ");
        String[] secondWords = secondText.split(" ");
        int wordCount = firstWords.length;
        byte minWords;
        if (wordCount <= 1) {
            return false;
        }

        wordCount = secondWords.length;
        minWords = 1;

        if (wordCount <= minWords) {
            return false;
        }

        int samePrefix = firstWords[0].equals(secondWords[0]) ? 1 : 0;
        return (boolean) (Object) (samePrefix != 0);
    }

    public static boolean isOwnTeamColor(DyeColor dyeColor) {
        if (dyeColor != null && mc.player != null) {
            EquipmentSlot[] slots = armorSlots;
            int slotCount = slots.length;
            int i = 0;

            while (i < slotCount) {
                EquipmentSlot equipmentSlot = slots[i];
                DyeColor dyeColor2 = getArmorDyeColor(mc.player.getItemBySlot(equipmentSlot));
                if (dyeColor2 != null) {
                    return dyeColor == dyeColor2;
                }

                i++;
            }

            return false;
        } else {
            return false;
        }
    }

    private static DyeColor getArmorDyeColor(ItemStack itemStack) {
        Object component = itemStack;
        if (itemStack == null) {
            return null;
        }

        component = itemStack;

        if (((ItemStack) component).isEmpty()) {
            return null;
        }

        component = itemStack.get(DataComponents.DYED_COLOR);

        DyedItemColor dyedItemColor = (DyedItemColor) component;
        DyedItemColor dyedItemColor2 = dyedItemColor;
        if (dyedItemColor == null) {
            return null;
        }

        dyedItemColor2 = dyedItemColor;

        return findNearestDyeColor(dyedItemColor2.rgb());
    }

    private static DyeColor findNearestDyeColor(int rgb) {

        int maskedRgb = rgb & 16777215;
        DyeColor dyeColor = null;
        int bestDistance = Integer.MAX_VALUE;
        DyeColorMapping[] mappings = dyeColorMappings;
        int mappingCount = mappings.length;
        int i = 0;

        while (i < mappingCount) {
            DyeColorMapping mapping = mappings[i];
            int distance = colorDistanceSquared(maskedRgb, mapping.rgb());
            if (distance < bestDistance) {
                bestDistance = distance;
                dyeColor = mapping.dyeColor();
            }

            i++;
        }

        Iterator dyeIterator = DyeColor.VALUES.iterator();

        while (dyeIterator.hasNext()) {
            label40:
            {
                DyeColor dyeColor2 = (DyeColor) dyeIterator.next();
                int dyeRgb = dyeColor2.getTextureDiffuseColor();

                i = colorDistanceSquared(maskedRgb, dyeRgb & 16777215);
                if (i >= bestDistance) {
                    break label40;
                }

                bestDistance = i;
                dyeColor = dyeColor2;
            }
        }

        int finalDistance = bestDistance;
        int maxDistance = 2500;

        return finalDistance <= maxDistance ? dyeColor : null;
    }

    private static int colorDistanceSquared(int colorA, int colorB) {

        int redShiftedA = colorA >> 16;

        int redA = redShiftedA & 0xFF;

        int redShiftedB = colorB >> 16;

        int redDiff = redA - (redShiftedB & 0xFF);

        int greenShiftedA = colorA >> 8;

        int greenA = greenShiftedA & 0xFF;

        int greenShiftedB = colorB >> 8;

        int greenDiff = greenA - (greenShiftedB & 0xFF);

        int blueA = colorA & 0xFF;

        int blueDiff = blueA - (colorB & 0xFF);
        return redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff;
    }

    public static boolean hasTeamColor(LivingEntity livingEntity) {
        LivingEntity livingEntity2 = livingEntity;
        if (livingEntity == mc.player) {
            return true;
        }

        livingEntity2 = livingEntity;

        AABB boundingBox = livingEntity2.getBoundingBox().inflate(0.5);
        List nearbyArmorStands =
                mc.level.getEntitiesOfClass(ArmorStand.class, boundingBox, armorStand -> true);
        ClientPacketListener clientPacketListener = mc.getConnection();
        PlayerInfo playerInfo = clientPacketListener.getPlayerInfo(mc.player.getUUID());
        PlayerInfo playerInfo2 = playerInfo;
        if (playerInfo == null) {
            return false;
        }

        playerInfo2 = playerInfo;

        PlayerTeam playerTeam = playerInfo2.getTeam();
        PlayerTeam playerTeam2 = playerTeam;
        if (playerTeam == null) {
            return false;
        }

        playerTeam2 = playerTeam;

        Optional teamColor = playerTeam2.getColor();
        Object colorHolder = teamColor;
        if (teamColor.isEmpty()) {
            return false;
        }

        colorHolder = teamColor.get();

        TextColor textColor2 = ((TeamColor) colorHolder).textColor();
        boolean noArmorStands = nearbyArmorStands.isEmpty();
        if (!noArmorStands) {
            for (Object element : nearbyArmorStands) {
                ArmorStand armorStand2 = (ArmorStand) element;
                Component component2 = armorStand2.getDisplayName();
                ArrayList<Component> nameParts = new ArrayList();
                nameParts.add(component2);
                boolean added = nameParts.addAll(component2.getSiblings());

                for (Component component3 : nameParts) {
                    Style style = component3.getStyle();
                    TextColor textColor3 = style.getColor();
                    TextColor textColor4 = textColor3;

                    label108:
                    {
                        if (textColor3 == null) {
                            break label108;
                        }

                        textColor4 = textColor3;

                        boolean sameColor = textColor4.equals(textColor2);

                        if (sameColor) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static String stripColorCodes(String text) {
        return colorCodePattern.matcher(text).replaceAll("");
    }
}
