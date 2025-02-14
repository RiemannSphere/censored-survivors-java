package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.IntColumn;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;
import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaChannel;

/**
 * Generates social media posts for a given set of customers.
 */
public class SocialMediaPostsGenerator {
    private final UniformRealDistribution meanDistribution = new UniformRealDistribution(1, 500);

    private record Post(String customerId, String customerName, SocialMediaChannel channel, int year, int week, int postCount) {}

    private class PostCountDistribution {
        private BinomialDistribution bernoulliDistribution;
        private LogNormalDistribution logNormalDistribution;
        
        /**
         * Generates a distribution for the number of posts for a given week.
         * 
         * @param mean Mean number of posts per week.
         * @param stdDev Standard deviation of the number of posts per week.
         * @param frequency Frequency of posting = 1 / number of weeks between posts.
         */ 
        public PostCountDistribution(SocialMediaPostDistributionParams params) {
            // Binomial distribution with n = 1 is equivalent to a Bernoulli distribution.
            this.bernoulliDistribution = new BinomialDistribution(1, params.frequency());
            // Convert normal parameters to log-normal parameters to stay close to the normal distribution values.
            double logNormalMean = Math.log(params.mean() * params.mean() / Math.sqrt(params.stdDev() * params.stdDev() + params.mean() * params.mean()));
            double logNormalStdDev = Math.sqrt(Math.log(1 + (params.stdDev() * params.stdDev()) / (params.mean() * params.mean())));
            this.logNormalDistribution = new LogNormalDistribution(logNormalMean, logNormalStdDev);
        }

        public int sample() {
            return (int) Math.round(
                this.bernoulliDistribution.sample() * 
                this.logNormalDistribution.sample()
            );
        }
    }

    private final Table customers;

    public SocialMediaPostsGenerator(Table customers) {
        this.customers = customers;
    }

    /**
     * Generates social media posts for a given set of customers.
     * Uses two distributions: 
     * - Log-normal distribution to generate the number of posts for a given week,
     * - Bernoulli distribution to choose whether to post or not for a given week.
     * (We use the log-normal distribution instead of normal to avoid negative values.)
     * 
     * Channels are chosen with a random subset weighted by channel popularity.
     * 
     * The first matching rule is used to generate the posts. If no rule is found,
     * a random mean, stdDev and frequency are sampled from the distributions and used to generate the posts.
     * 
     * @param tableName Name of the table to save the generated posts to.
     * @param postRules Rules for generating the posts.
     * @return Table with the generated posts.
     */
    public Table generatePosts(String tableName, List<SocialMediaPostRule> postRules) {
        Table df = Table.create(tableName);
        df.addColumns(
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN),
            StringColumn.create(ProjectConfig.CHANNEL_COLUMN),
            IntColumn.create(ProjectConfig.YEAR_COLUMN),
            IntColumn.create(ProjectConfig.WEEK_COLUMN),
            IntColumn.create(ProjectConfig.POST_COUNT_COLUMN)
        );

        this.customers.stream().flatMap(customerRow -> {
            String customerId = customerRow.getString(ProjectConfig.CUSTOMER_ID_COLUMN);
            String customerName = customerRow.getString(ProjectConfig.CUSTOMER_NAME_COLUMN);
            String industry = customerRow.getString(ProjectConfig.INDUSTRY_COLUMN);
            String country = customerRow.getString(ProjectConfig.COUNTRY_COLUMN);
            String plan = customerRow.getString(ProjectConfig.PLAN_COLUMN);
            LocalDate startDate = customerRow.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
            LocalDate endDate = customerRow.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);

            List<SocialMediaChannel> channels = SocialMediaChannel.getRandomChannelsSubset();

            return startDate.datesUntil(endDate)
            .filter(date -> date.getDayOfWeek() == DayOfWeek.MONDAY) // samples are weeks
            .flatMap(date -> channels.stream().map(channel -> {
                PostCountDistribution firstMatchingDistribution = postRules.stream()
                    .filter(rule -> {
                        boolean matchesChannel = rule.param() == SocialMediaParam.CHANNEL && rule.paramValue().equals(channel.getDisplayName());
                        boolean matchesIndustry = rule.param() == SocialMediaParam.INDUSTRY && rule.paramValue().equals(industry);
                        boolean matchesCountry = rule.param() == SocialMediaParam.COUNTRY && rule.paramValue().equals(country);
                        boolean matchesPlan = rule.param() == SocialMediaParam.PLAN && rule.paramValue().equals(plan);

                        return matchesChannel || matchesIndustry || matchesCountry || matchesPlan;
                    })
                    .map(rule -> new PostCountDistribution(rule.postCountDistributionParams()))
                    .findFirst()
                    .orElseGet(() -> {
                        double randomMean = Math.max(1, meanDistribution.sample());
                        double randomStdDev = Math.max(1, randomMean * ProjectConfig.RANDOM.nextDouble());
                        double randomFrequency = Math.max(0.1, ProjectConfig.RANDOM.nextDouble());

                        return new PostCountDistribution(
                            new SocialMediaPostDistributionParams(randomMean, randomStdDev, randomFrequency));
                    });

                return new Post(
                    customerId, 
                    customerName, 
                    channel, 
                    date.getYear(), 
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), 
                    firstMatchingDistribution.sample()
                );
            }));
        }).forEach(post -> {
            df.stringColumn(ProjectConfig.CUSTOMER_ID_COLUMN).append(post.customerId());
            df.stringColumn(ProjectConfig.CUSTOMER_NAME_COLUMN).append(post.customerName());
            df.stringColumn(ProjectConfig.CHANNEL_COLUMN).append(post.channel().getDisplayName());
            df.intColumn(ProjectConfig.YEAR_COLUMN).append(post.year());
            df.intColumn(ProjectConfig.WEEK_COLUMN).append(post.week());
            df.intColumn(ProjectConfig.POST_COUNT_COLUMN).append(post.postCount());
        });

        return df;
    }
}
