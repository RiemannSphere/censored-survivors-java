package com.censoredsurvivors.data.model;

import java.util.Arrays;

public enum SocialMediaCountry {
    UNITED_STATES("United States"),
    UNITED_KINGDOM("United Kingdom"),
    GERMANY("Germany"),
    FRANCE("France"),
    ITALY("Italy"),
    GREECE("Greece"),
    TURKEY("Turkey");

    private String displayName;

    SocialMediaCountry(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
            .map(SocialMediaCountry::getDisplayName)
            .toArray(String[]::new);
    }
}
