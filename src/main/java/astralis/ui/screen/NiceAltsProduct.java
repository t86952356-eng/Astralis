package astralis.ui.screen;

public final class NiceAltsProduct {
    final String name;
    final String description;
    final int productId;
    final boolean custom;
    final String generatorCategory;

    NiceAltsProduct(String name, String description, int productId) {

        this(name, description, productId, false, "Unbanned");
    }

    NiceAltsProduct(String name, String description, int productId, boolean custom) {

        this(name, description, productId, custom, "Unbanned");
    }

    NiceAltsProduct(
            String name,
            String description,
            int productId,
            boolean custom,
            String generatorCategory) {
        this.name = name;
        this.description = description;
        this.productId = productId;
        this.custom = custom;
        this.generatorCategory = generatorCategory;
    }
}
