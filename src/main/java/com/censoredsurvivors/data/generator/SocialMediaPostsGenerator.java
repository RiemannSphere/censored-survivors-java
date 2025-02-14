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
import org.apache.commons.math3.util.Pair;

import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;
import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaChannel;

/**
 * Generates social media posts for a given set of customers.
 
 * Uses two distributions: 
 * - Log-normal distribution to generate the number of posts for a given week,
 * - Bernoulli distribution to choose whether to post or not for a given week.
 * (We use the log-normal distribution instead of normal to avoid negative values.)
 * 
 * Channels are chosen with a random subset weighted by channel popularity.
 */
public class SocialMediaPostsGenerator {

    private record Post(String customerId, String customerName, String channel, int year, int week, int postCount) {}

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
            List<SocialMediaChannel> channels = SocialMediaChannel.getRandomChannelsSubset();
            LocalDate startDate = customerRow.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
            LocalDate endDate = customerRow.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);

            // FIXME: This is combining for example all channels together which is wrong.
            double[] postCountDistributionParams = postRules.stream()
                .filter(rule -> {
                    SocialMediaParam param = rule.param();
                    String value = rule.paramValue();
                    SocialMediaPostDistributionParams params = rule.postCountDistributionParams();

                    boolean matchesChannel = param == SocialMediaParam.CHANNEL && channels.contains(SocialMediaChannel.getByDisplayName(value));
                    boolean matchesIndustry = param == SocialMediaParam.INDUSTRY && customerRow.getString(ProjectConfig.INDUSTRY_COLUMN).equals(value);
                    boolean matchesCountry = param == SocialMediaParam.COUNTRY && customerRow.getString(ProjectConfig.COUNTRY_COLUMN).equals(value);
                    boolean matchesPlan = param == SocialMediaParam.PLAN && customerRow.getString(ProjectConfig.PLAN_COLUMN).equals(value);

                    return matchesChannel || matchesIndustry || matchesCountry || matchesPlan;
                })
                .map(rule -> {
                    SocialMediaPostDistributionParams params = rule.postCountDistributionParams();

                    return new double[] {params.mean(), params.stdDev(), params.frequency(), 1};
                })
                .reduce(new double[] {0, 0, 0, 0}, (acc, params) -> {
                    acc[0] += params[0]; // sum of means
                    acc[1] += params[1]; // sum of std devs
                    acc[2] += params[2]; // sum of frequencies
                    acc[3] += params[3]; // number of rules

                    return acc;
                });
            
            double combinedMean = postCountDistributionParams[0] / postCountDistributionParams[3];
            double combinedStdDev = postCountDistributionParams[1] / postCountDistributionParams[3];
            double combinedFrequency = postCountDistributionParams[2] / postCountDistributionParams[3];
        
            PostCountDistribution postCountDistribution = new PostCountDistribution(
                new SocialMediaPostDistributionParams(combinedMean, combinedStdDev, combinedFrequency)
            );

            return startDate.datesUntil(endDate)
            .filter(date -> date.getDayOfWeek() == DayOfWeek.MONDAY) // samples are weeks
            .map(date -> {
                int postCount = postCountDistribution.sample();
                
                return new Post(customerId, customerName, channel, date.getYear(), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), postCount);
            });
        }).forEach(post -> {
            df.stringColumn(ProjectConfig.CUSTOMER_ID_COLUMN).append(post.customerId());
            df.stringColumn(ProjectConfig.CUSTOMER_NAME_COLUMN).append(post.customerName());
            df.stringColumn(ProjectConfig.CHANNEL_COLUMN).append(post.channel());
            df.intColumn(ProjectConfig.YEAR_COLUMN).append(post.year());
            df.intColumn(ProjectConfig.WEEK_COLUMN).append(post.week());
            df.intColumn(ProjectConfig.POST_COUNT_COLUMN).append(post.postCount());
        });

        return df;
    }
}
