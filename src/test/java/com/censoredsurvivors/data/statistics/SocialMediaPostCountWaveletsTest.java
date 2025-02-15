package com.censoredsurvivors.data.statistics;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import tech.tablesaw.api.Table;

import com.censoredsurvivors.data.generator.SocialMediaCustomerGenerator;
import com.censoredsurvivors.data.generator.SocialMediaPostsGenerator;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;
import com.censoredsurvivors.data.model.SocialMediaPostRule;

@TestInstance(Lifecycle.PER_CLASS)
public class SocialMediaPostCountWaveletsTest {
    private static final int NUMBER_OF_CUSTOMERS = 1;
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
    public void testTransform() {
        Table posts = postsGenerator.generatePosts("Platform Posts", postRules);

        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] coefficients = wavelets.transform(posts);

        System.out.println(Arrays.toString(coefficients));
    }
}
