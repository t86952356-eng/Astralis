package astralis.module.impl.arraylist;

import pub.astralis.client.module.core.Category;

public class CategorySwitchMap {
    public static int[] categoryOrdinalMap;

    static {
        categoryOrdinalMap = new int[Category.values().length];

        try {
            categoryOrdinalMap[Category.Client.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Combat.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Ghost.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Movement.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Player.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.World.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Visuals.ordinal()] = 7;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            categoryOrdinalMap[Category.Misc.ordinal()] = 8;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
