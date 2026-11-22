package astralis.metadata;

public class ModuleMetadata {
    private final String id;
    private final String name;
    private final String description;
    private final String category;

    public ModuleMetadata(String id, String name, String description, String category) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public ModuleMetadata(String name, String description) {
        this("", name, description, "MISC");
    }

    public ModuleMetadata(String name, String description, String category) {
        this("", name, description, category);
    }

    public String getDescription() {
        return this.description;
    }

    public String getCategory() {
        return this.category;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
