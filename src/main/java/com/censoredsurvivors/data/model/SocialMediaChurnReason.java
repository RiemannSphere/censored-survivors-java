package com.censoredsurvivors.data.model;

import java.util.Arrays;

public enum SocialMediaChurnReason {
    POST_COUNT_DROP("Post count drop");

    private String displayName;

    SocialMediaChurnReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
            .map(SocialMediaChurnReason::getDisplayName)
            .toArray(String[]::new);
    }
}
