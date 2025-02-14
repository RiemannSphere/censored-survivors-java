package com.censoredsurvivors.data.model;

import java.util.Arrays;

public enum SocialMediaPlan {
    BASIC("Basic"),
    PRO("Pro"),
    ENTERPRISE("Enterprise");

    private String displayName;

    SocialMediaPlan(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
            .map(SocialMediaPlan::getDisplayName)
            .toArray(String[]::new);
    }
}
