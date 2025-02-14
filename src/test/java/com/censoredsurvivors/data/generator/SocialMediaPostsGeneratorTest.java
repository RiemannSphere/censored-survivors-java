package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.IntColumn;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaCountry;
import com.censoredsurvivors.data.model.SocialMediaIndustry;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPlan;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;

@TestInstance(Lifecycle.PER_CLASS)
public class SocialMediaPostsGeneratorTest {

    private static final int NUMBER_OF_CUSTOMERS = 100;
    private static final double PROPORTION_OF_CUSTOMERS_WITH_POSTS = 0.1;
    private static final double PROPORTION_OF_POSTS_ON_PLATFORM = 0.1;
    private static final int NUMBER_OF_YEARS = 5;

    private SocialMediaPostsGenerator postsGenerator;

    private Table customers;
    private List<SocialMediaPostRule> postRules;

    @BeforeAll
    public void setUp() {
        SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator();
        customers = customerGenerator.generateCustomers(NUMBER_OF_CUSTOMERS, PROPORTION_OF_CUSTOMERS_WITH_POSTS, PROPORTION_OF_POSTS_ON_PLATFORM, NUMBER_OF_YEARS);
        postsGenerator = new SocialMediaPostsGenerator(customers);
        postRules = List.of(
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 1.0)),
            new SocialMediaPostRule(SocialMediaParam.INDUSTRY, SocialMediaIndustry.ENERGY.name(), new SocialMediaPostDistributionParams(10, 5, 0.33)),
            new SocialMediaPostRule(SocialMediaParam.COUNTRY, SocialMediaCountry.GERMANY.name(), new SocialMediaPostDistributionParams(50, 5, 0.75)),
            new SocialMediaPostRule(SocialMediaParam.PLAN, SocialMediaPlan.BASIC.name(), new SocialMediaPostDistributionParams(5, 1, 1.0))
        );
    }

    @Test
    public void testGeneratePosts() {
        int expectedPosts = this.customers.stream()
            .mapToInt(row -> {
                LocalDate start = row.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
                LocalDate end = row.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);
                return (int) start.datesUntil(end)
                    .filter(date -> date.getDayOfWeek() == DayOfWeek.MONDAY)
                    .count();
            })
            .sum();

        Table posts = postsGenerator.generatePosts("Platform Posts", postRules);
        Assertions.assertEquals(expectedPosts, posts.rowCount(), "Number of posts generated should be equal to the number of Mondays between the contract start and end dates for each customer.");


    }
}
