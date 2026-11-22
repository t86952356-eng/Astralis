package pub.astralis.client.module.core;

public enum Category {
    Combat("Combat"),
    Movement("Movement"),
    Player("Player"),
    World("World"),
    Visuals("Visuals"),
    Ghost("Ghost"),
    Misc("Misc"),
    Client("Client");

    private final String name;
    public static final Category COMBAT = Combat;
    public static final Category MOVEMENT = Movement;
    public static final Category PLAYER = Player;
    public static final Category WORLD = World;
    public static final Category VISUAL = Visuals;
    public static final Category GHOST = Ghost;
    public static final Category MISC = Misc;
    public static final Category CLIENT = Client;

    public String getName() {
        return this.name;
    }

    Category(String displayName) {
        this.name = displayName;
    }
}
