package com.censoredsurvivors.util;

import java.util.Random;
import java.time.LocalDate;

public class ProjectConfig {
    public static final long RANDOM_SEED = 42L;
    public static final Random RANDOM = new Random(RANDOM_SEED);

    public static final LocalDate OBSERVATION_START_DATE = LocalDate.of(2020, 1, 1);
    public static final int EXTENDED_PERIOD_YEARS = 5;

    // Make sure the arrays have different prime number of elements to avoid patterns
    public static final String[] INDUSTRY_VALUES = {
        "Technology",
        "Finance",
        "Healthcare",
        "Manufacturing",
        "Retail",
        "Energy",
        "Transportation",
        "Telecom",
        "Entertainment",
        "Education",
        "Social Media"
    };
    public static final String[] COUNTRY_VALUES = {
        "United States",
        "United Kingdom",
        "Germany",
        "France",
        "Italy",
        "Greece",
        "Turkey",
    };
    public static final String[] CHANNEL_VALUES = {
        "Facebook",
        "Twitter",
        "Instagram",
        "LinkedIn",
        "YouTube",
    };
    public static final String[] PLAN_VALUES = {
        "Basic",
        "Pro",
        "Enterprise"
    };
}
