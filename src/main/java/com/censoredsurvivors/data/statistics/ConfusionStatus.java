package com.censoredsurvivors.data.statistics;

public enum ConfusionStatus {
    TRUE_POSITIVE("True Positive"),
    FALSE_POSITIVE("False Positive"),
    TRUE_NEGATIVE("True Negative"),
    FALSE_NEGATIVE("False Negative");

    private final String displayName;

    private ConfusionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
