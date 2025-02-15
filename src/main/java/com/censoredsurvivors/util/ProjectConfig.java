package com.censoredsurvivors.util;

import java.util.Random;
import java.time.LocalDate;

import com.censoredsurvivors.data.model.SocialMediaIndustry;
import com.censoredsurvivors.data.model.SocialMediaCountry;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaPlan;

public class ProjectConfig {
    // Random seed for reproducibility. Changing it might break the tests.💩
    public static final long RANDOM_SEED = 41L;
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

    // Make sure the params have different prime number of elements to avoid patterns
    public static final String[] INDUSTRY_VALUES = SocialMediaIndustry.getAllDisplayNames();
    public static final String[] COUNTRY_VALUES = SocialMediaCountry.getAllDisplayNames();
    public static final String[] CHANNEL_VALUES = SocialMediaChannel.getAllDisplayNames();
    public static final String[] PLAN_VALUES = SocialMediaPlan.getAllDisplayNames();
}
