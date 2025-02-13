package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.IntColumn;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;

import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaPostsGenerator {

    private record Post(String customerId, String customerName, String channel, int year, int week, int postCount) {}

    private class PostCountDistribution {
        private BinomialDistribution bernoulliDistribution;
        private LogNormalDistribution logNormalDistribution;
        
        /**
         * Uses two distributions: 
         * - Log-normal distribution to generate the number of posts for a given week,
         * - Bernoulli distribution to choose whether to post or not for a given week.
         * (We use the log-normal distribution instead of normal to avoid negative values.)
         * 
         * @param mean Mean number of posts per week.
         * @param stdDev Standard deviation of the number of posts per week.
         * @param frequency Frequency of posting = 1 / number of weeks between posts.
         */ 
        public PostCountDistribution(double mean, double stdDev, double frequency) {
            // Binomial distribution with n = 1 is equivalent to a Bernoulli distribution.
            this.bernoulliDistribution = new BinomialDistribution(1, frequency);
            // Convert normal parameters to log-normal parameters to stay close to the normal distribution values.
            double logNormalMean = Math.log(mean * mean / Math.sqrt(stdDev * stdDev + mean * mean));
            double logNormalStdDev = Math.sqrt(Math.log(1 + (stdDev * stdDev) / (mean * mean)));
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

    public Table generatePosts(String tableName) {
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
            String channel = ProjectConfig.CHANNEL_VALUES[0]; // TODO: vary channels
            LocalDate startDate = customerRow.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
            LocalDate endDate = customerRow.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);
        
            // TODO: vary the parameters of the distribution for different customer cohorts
            PostCountDistribution postCountDistribution = new PostCountDistribution(10, 5, 0.33);

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
