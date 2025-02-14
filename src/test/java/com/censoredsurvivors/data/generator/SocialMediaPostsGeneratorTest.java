package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import tech.tablesaw.api.Table;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaParam;
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
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.name(), new SocialMediaPostDistributionParams(100, 50, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.TWITTER.name(), new SocialMediaPostDistributionParams(10, 1, 0.8)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.LINKEDIN.name(), new SocialMediaPostDistributionParams(5, 2, 0.25))
        );
    }

    @Test
    public void testGeneratePosts() {
        int minimumExpectedPosts = this.customers.stream()
            .mapToInt(row -> {
                LocalDate start = row.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
                LocalDate end = row.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);

                return (int) start.datesUntil(end)
                    .filter(date -> date.getDayOfWeek() == DayOfWeek.MONDAY)
                    .count();
            })
            .sum();

        Table posts = postsGenerator.generatePosts("Platform Posts", postRules);
        // Post every Monday between the contract start and end dates for each customer.
        // Some customers post on more than one channel, therefore the number of posts can be higher than the minimum expected.
        Assertions.assertTrue(minimumExpectedPosts <= posts.rowCount(), 
            String.format("Expected at least %d posts but found %d posts", minimumExpectedPosts, posts.rowCount()));
    }
}
