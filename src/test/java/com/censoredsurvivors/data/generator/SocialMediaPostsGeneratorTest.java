package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;

import tech.tablesaw.api.Table;
import java.time.LocalDate;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.IntStream;

import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;

public class SocialMediaPostsGeneratorTest {

    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final double PROPORTION_OF_LEFT_CENSORED_CUSTOMERS = 0.1;
    private static final double PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS = 0.1;
    private static final int NUMBER_OF_YEARS = 5;

    @Test
    public void testGeneratePosts() {
        SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator();
        Table customers = customerGenerator.generateCustomers(
            NUMBER_OF_CUSTOMERS, 
            PROPORTION_OF_LEFT_CENSORED_CUSTOMERS, 
            PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS, 
            NUMBER_OF_YEARS
        );
        SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
        List<SocialMediaPostRule> postRules = List.of(
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.name(), new SocialMediaPostDistributionParams(100, 50, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.TWITTER.name(), new SocialMediaPostDistributionParams(10, 1, 0.8)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.LINKEDIN.name(), new SocialMediaPostDistributionParams(5, 2, 0.25))
        );
        int minimumExpectedPosts = customers.stream()
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

    @Test
    public void testPlotPosts() throws IOException {
        SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator();
        Table singleCustomer = customerGenerator.generateCustomers(NUMBER_OF_CUSTOMERS, PROPORTION_OF_LEFT_CENSORED_CUSTOMERS, PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS, NUMBER_OF_YEARS);
        SocialMediaPostsGenerator singleCustomerPostsGenerator = new SocialMediaPostsGenerator(singleCustomer);
        List<SocialMediaPostRule> postRules = List.of(
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.name(), new SocialMediaPostDistributionParams(100, 50, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.TWITTER.name(), new SocialMediaPostDistributionParams(10, 1, 0.8)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.LINKEDIN.name(), new SocialMediaPostDistributionParams(5, 2, 0.25))
        );
        Table singleCustomerPosts = singleCustomerPostsGenerator.generatePosts("Platform Posts", postRules);

        // Group by week and sum post counts
        Table weeklyPosts = singleCustomerPosts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        // Create XY line chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(600)
            .title("Weekly Post Counts")
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        // Convert data to arrays for plotting
        double[] weeks = IntStream.range(0, weeklyPosts.rowCount())
            .mapToDouble(i -> i)
            .toArray();
        
        double[] postCounts = weeklyPosts.doubleColumn("Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]")
            .asDoubleArray();

        // Add the data series
        chart.addSeries("Posts", weeks, postCounts)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        // Customize chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setYAxisMin(0.0);

        // Save chart
        BitmapEncoder.saveBitmap(
            chart, 
            "target/test-output/weekly-posts.png", 
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
