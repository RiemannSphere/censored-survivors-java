package com.censoredsurvivors.util;

import java.util.Random;
import java.time.LocalDate;

public class ProjectConfig {
    public static final long RANDOM_SEED = 42L;
    public static final Random RANDOM = new Random(RANDOM_SEED);

    public static final LocalDate OBSERVATION_START_DATE = LocalDate.of(2020, 1, 1);
    public static final int EXTENDED_PERIOD_YEARS = 5;

    public static final String CUSTOMER_ID_COLUMN = "customerId";
    public static final String CUSTOMER_NAME_COLUMN = "customerName";
    public static final String CONTRACT_START_DATE_COLUMN = "contractStartDate";
    public static final String CONTRACT_END_DATE_COLUMN = "contractEndDate";
    public static final String INDUSTRY_COLUMN = "industry";
    public static final String COUNTRY_COLUMN = "country";
    public static final String CHANNEL_COLUMN = "channel";
    public static final String YEAR_COLUMN = "year";
    public static final String WEEK_COLUMN = "week";
    public static final String POST_COUNT_COLUMN = "postCount";
    public static final String PLAN_COLUMN = "plan";

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
