package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;

import tech.tablesaw.api.Table;
import java.time.LocalDate;
import java.awt.Color;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.awt.BasicStroke;
import java.time.temporal.WeekFields;
import java.time.format.DateTimeFormatter;

import com.censoredsurvivors.util.ProjectConfig;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;

class PostsTestSetupSingleton {
    private static final boolean ALL_CUSTOMERS_FULL_LIFETIME = true; // ensure enough posts are generated
    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final double PROPORTION_OF_LEFT_CENSORED_CUSTOMERS = 0.1;
    private static final double PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS = 0.1;
    private static final int NUMBER_OF_YEARS = 5;

    private static Table posts;
    private static Table customers;

    public static Table[] getPosts() {
        if (customers == null) {
            SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator(ALL_CUSTOMERS_FULL_LIFETIME);
            customers = customerGenerator.generateCustomers(
                NUMBER_OF_CUSTOMERS, 
                PROPORTION_OF_LEFT_CENSORED_CUSTOMERS, 
                PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS, 
                NUMBER_OF_YEARS
            );
        }

        if (posts == null) {
            SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
            List<SocialMediaPostRule> postRules = List.of(
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.getDisplayName(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.getDisplayName(), new SocialMediaPostDistributionParams(100, 50, 0.5)),
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.TWITTER.getDisplayName(), new SocialMediaPostDistributionParams(10, 1, 0.8)),
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.LINKEDIN.getDisplayName(), new SocialMediaPostDistributionParams(5, 2, 0.25))
            );
            // define all channels so there will be no random channel selection
            List<SocialMediaChannel> channels = List.of(
                SocialMediaChannel.FACEBOOK,
                SocialMediaChannel.INSTAGRAM,
                SocialMediaChannel.TWITTER,
                SocialMediaChannel.LINKEDIN
            );
            posts = postsGenerator.generatePosts("Platform Posts", postRules, channels);
        }

        return new Table[]{ posts, customers };
    }
}

class PostsTestSetupSingletonWithChurn {
    private static final boolean ALL_CUSTOMERS_FULL_LIFETIME = true; // ensure enough posts are generated
    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final int NUMBER_OF_YEARS = 5;
    private static final double CHURN_PROBABILITY = 1.0;

    private static Table posts;
    private static Table customers;
    
    public static Table[] getPosts() {
        if (customers == null) {
            SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator(ALL_CUSTOMERS_FULL_LIFETIME);
            customers = customerGenerator.generateUncensoredCustomers(NUMBER_OF_CUSTOMERS, NUMBER_OF_YEARS, CHURN_PROBABILITY);
        }

        if (posts == null) {
            SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
            // only generate posts for one channel to make churn more visible
            List<SocialMediaPostRule> postRules = List.of(
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.getDisplayName(), new SocialMediaPostDistributionParams(200, 5, 0.8))
            );
            List<SocialMediaChannel> channels = List.of(SocialMediaChannel.FACEBOOK);
            posts = postsGenerator.generatePosts("Platform Posts", postRules, channels);
        }

        return new Table[]{ posts, customers };
    }
}

public class SocialMediaPostsGeneratorTest {
    // @Test
    public void testGeneratePosts() {
        Table[] postsAndCustomers = PostsTestSetupSingleton.getPosts();
        Table posts = postsAndCustomers[0];
        Table customers = postsAndCustomers[1];

        int minimumExpectedPosts = customers.stream()
            .mapToInt(row -> {
                LocalDate start = row.getDate(ProjectConfig.CONTRACT_START_DATE_COLUMN);
                LocalDate end = row.getDate(ProjectConfig.CONTRACT_END_DATE_COLUMN);

                return (int) start.datesUntil(end)
                    .filter(date -> date.getDayOfWeek() == DayOfWeek.MONDAY)
                    .count();
            })
            .sum();

        // Post every Monday between the contract start and end dates for each customer.
        // Some customers post on more than one channel, therefore the number of posts can be higher than the minimum expected.
        Assertions.assertTrue(minimumExpectedPosts <= posts.rowCount(), 
            String.format("Expected at least %d posts but found %d posts", minimumExpectedPosts, posts.rowCount()));
    }

    // @Test
    public void testPlotPosts() throws IOException {
        Table[] postsAndCustomers = PostsTestSetupSingleton.getPosts();
        Table posts = postsAndCustomers[0];
        Table customers = postsAndCustomers[1];

        // Group by week and sum post counts
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        LocalDate startDate = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN).min();
        LocalDate endDate = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN).max();

        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(600)
            .title("Weekly Post Counts")
            .xAxisTitle(String.format("Week (from %s to %s)",
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ))
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

    // @Test
    public void testPlotPostsHistogram() throws IOException {
        Table[] postsAndCustomers = PostsTestSetupSingleton.getPosts();
        Table posts = postsAndCustomers[0];

        // Convert post counts to a List<Double>
        List<Double> postCounts = posts.intColumn(ProjectConfig.POST_COUNT_COLUMN)
            .asList()
            .stream()
            .filter(postCount -> postCount > 0)
            .map(Integer::doubleValue)
            .collect(Collectors.toList());

        // Create histogram
        double minValue = postCounts.stream().mapToDouble(d -> d).min().orElse(0);
        double maxValue = postCounts.stream().mapToDouble(d -> d).max().orElse(0);
        Histogram histogram = new Histogram(postCounts, 48, minValue, maxValue);

        // Create chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title("Post Count Distribution")
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setXAxisLabelRotation(-45);
        chart.getStyler().setDecimalPattern("#");
        chart.getStyler().setYAxisMin(0.0);

        // Add histogram series
        chart.addSeries(
            String.format(Locale.US, "Posts [n=%d]", postCounts.size()),
            histogram.getxAxisData(),
            histogram.getyAxisData()
        ).setFillColor(new Color(0, 116, 217, 128));  // Blue

        // Save chart
        BitmapEncoder.saveBitmap(
            chart, 
            "target/test-output/weekly-posts-histogram.png", 
            BitmapEncoder.BitmapFormat.PNG
        );
    }

    @Test
    public void testPlotPostsWithChurn() throws IOException {
        Table[] postsAndCustomers = PostsTestSetupSingletonWithChurn.getPosts();
        Table posts = postsAndCustomers[0];
        Table customers = postsAndCustomers[1];

        LocalDate startDate = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN).min();
        LocalDate endDate = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN).max();
        LocalDate churnDate = customers.dateColumn(ProjectConfig.CHURN_DATE_COLUMN).get(0);

        Assertions.assertNotNull(churnDate, "Churn date is null");

        // Group by week and sum post counts
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN); 
        
        // Create XY line chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(600)
            .title("Weekly Post Counts")
            .xAxisTitle(String.format("Week (from %s to %s)",
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ))
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

        // Add vertical line for churn date
        int churnYear = churnDate.getYear();
        int churnWeek = churnDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        double churnWeekIndex = churnWeek - 1 + (churnYear - weeklyPosts.intColumn(ProjectConfig.YEAR_COLUMN).min()) * 52;

        double maxPosts = Math.ceil(posts.intColumn(ProjectConfig.POST_COUNT_COLUMN).max() * 1.1);
        chart.addSeries(
            "Churn Date: " + churnDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            new double[]{churnWeekIndex, churnWeekIndex}, 
            new double[]{0, maxPosts}
        )
            .setLineStyle(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f}, 0.0f))
            .setLineColor(Color.RED)
            .setShowInLegend(true);

        // Customize chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setYAxisMin(0.0); 

        // Save chart
        BitmapEncoder.saveBitmap(
            chart, 
            "target/test-output/weekly-posts-with-churn.png", 
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
