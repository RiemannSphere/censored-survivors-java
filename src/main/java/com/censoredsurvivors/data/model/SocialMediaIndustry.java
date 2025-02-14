package com.censoredsurvivors.data.model;

import java.util.Arrays;

public enum SocialMediaIndustry {
    TECHNOLOGY("Technology"),
    FINANCE("Finance"),
    HEALTHCARE("Healthcare"),
    MANUFACTURING("Manufacturing"),
    RETAIL("Retail"),
    ENERGY("Energy"),
    TRANSPORTATION("Transportation"),
    TELECOM("Telecom"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    SOCIAL_MEDIA("Social Media");

    private String displayName;

    SocialMediaIndustry(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
            .map(SocialMediaIndustry::getDisplayName)
            .toArray(String[]::new);
    }
}
