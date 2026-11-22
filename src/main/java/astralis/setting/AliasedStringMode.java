package astralis.setting;

import java.util.Arrays;
import java.util.List;

public class AliasedStringMode extends StringMode {
    private final List<String> aliasNames;

    public AliasedStringMode(String name, String... aliases) {
        super(name);
        this.aliasNames =
                Arrays.stream(aliases)
                        .filter(
                                alias -> {
                                    return alias != null && !alias.isBlank();
                                })
                        .toList();
    }

    public List<String> getAliasNames() {
        return this.aliasNames;
    }

    public boolean aliases(String name) {
        return this.aliasNames.stream().anyMatch(alias -> alias.equalsIgnoreCase(name));
    }
}
