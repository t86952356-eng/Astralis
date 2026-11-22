package astralis.ui.component;

import lombok.Generated;

public enum PackSortOrder {
    NEWEST("newol", "Newest to Oldest"),
    OLDEST("olnew", "Oldest to Newest"),
    NAME_ASCENDING("az", "Name A-Z"),
    NAME_DESCENDING("za", "Name Z-A"),
    MOST_DOWNLOADED("dd", "Most Downloaded"),
    LEAST_DOWNLOADED("da", "Least Downloaded");

    private final String sortCode;
    private final String displayName;

    @Generated
    public String getSortCode() {
        return this.sortCode;
    }

    @Generated
    public String getDisplayName() {
        return this.displayName;
    }

    @Generated
    PackSortOrder(String sortCode, String displayName) {
        this.sortCode = sortCode;
        this.displayName = displayName;
    }
}
