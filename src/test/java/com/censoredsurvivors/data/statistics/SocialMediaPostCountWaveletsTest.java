package com.censoredsurvivors.data.statistics;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tech.tablesaw.api.Table;

import com.censoredsurvivors.data.generator.SocialMediaCustomerGenerator;
import com.censoredsurvivors.data.generator.SocialMediaPostsGenerator;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.util.ProjectConfig;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.time.temporal.WeekFields;

class WaveletTestSetupSingleton {
    private static final boolean ALL_CUSTOMERS_FULL_LIFETIME = true; // ensure enough posts are generated
    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final int NUMBER_OF_YEARS = 5;

    private static Table posts;

    public static Table getPosts() {
        if (posts == null) {
            SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator(ALL_CUSTOMERS_FULL_LIFETIME);
            Table customers = customerGenerator.generateUncensoredCustomers(NUMBER_OF_CUSTOMERS, NUMBER_OF_YEARS);
            SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
            List<SocialMediaPostRule> postRules = List.of(
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.getDisplayName(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.getDisplayName(), new SocialMediaPostDistributionParams(100, 50, 0.5))
            );
            List<SocialMediaChannel> channels = List.of(SocialMediaChannel.FACEBOOK, SocialMediaChannel.INSTAGRAM);
    
            posts = postsGenerator.generatePosts("Platform Posts", postRules, channels);
        }

        return posts;
    }
}

class WaveletWithChurnTestSetupSingleton {
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
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.getDisplayName(), new SocialMediaPostDistributionParams(200, 50, 0.5))
            );
            List<SocialMediaChannel> channels = List.of(SocialMediaChannel.FACEBOOK);
            posts = postsGenerator.generatePosts("Platform Posts", postRules, channels);
        }

        return new Table[]{ posts, customers };
    }
}

public class SocialMediaPostCountWaveletsTest {

    @Test
    public void testTransform() {
        Table posts = WaveletTestSetupSingleton.getPosts();
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] coefficients = wavelets.transform(posts);

        // Calculate the next power of 2 after posts.rowCount()
        int nextPowerOfTwo = 1 << (32 - Integer.numberOfLeadingZeros(posts.rowCount() - 1));
        Assertions.assertEquals(nextPowerOfTwo, coefficients.length);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    public void testTransformAndPlotFrequency(int frequencyLevel) throws IOException {
        Table posts = WaveletTestSetupSingleton.getPosts();
        
        // Create output directory
        new java.io.File("target/test-output/wavelets/normal").mkdirs();
        
        // Group by week and sum post counts, then filter out empty weeks
        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        // Create two separate charts
        XYChart originalChart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Original Weekly Post Counts")
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        XYChart reconstructionChart = new XYChartBuilder()
            .width(800)
            .height(200)
            .title(String.format("Level %d Wavelet Reconstruction", frequencyLevel))
            .xAxisTitle("Week")
            .yAxisTitle("Amplitude")
            .build();

        // Convert data to arrays for plotting
        double[] weeks = IntStream.range(0, weeklyPosts.rowCount())
            .mapToDouble(i -> i)
            .toArray();
        
        double[] originalPostCounts = weeklyPosts.doubleColumn(summaryColumnName)
            .asDoubleArray();

        // Add the original data series
        originalChart.addSeries(
            "Original Posts", 
            weeks, 
            originalPostCounts
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        // Create a new table with just the summary column for the wavelet transform
        Table transformTable = Table.create("Transform Input")
            .addColumns(
                weeklyPosts.doubleColumn(summaryColumnName)
                    .asIntColumn()
                    .setName(ProjectConfig.POST_COUNT_COLUMN)
            );

        // Add wavelet reconstruction for specified level
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] levelReconstruction = wavelets.transformAndIsolateFrequency(transformTable, frequencyLevel);

        // Truncate the reconstructed data to match the original length
        double[] truncatedReconstruction = Arrays.copyOfRange(
            levelReconstruction, 
            levelReconstruction.length - weeks.length, 
            levelReconstruction.length
        );

        // Add the reconstructed series
        reconstructionChart.addSeries(
            "Reconstruction", 
            weeks, 
            truncatedReconstruction
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        // Customize charts
        for (XYChart chart : new XYChart[]{originalChart, reconstructionChart}) {
            chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
            chart.getStyler().setMarkerSize(0);
            chart.getStyler().setPlotGridLinesVisible(false);
        }
        originalChart.getStyler().setYAxisMin(0.0);

        // Create a combined image
        var originalImage = BitmapEncoder.getBufferedImage(originalChart);
        var reconstructionImage = BitmapEncoder.getBufferedImage(reconstructionChart);
        
        var combinedImage = new java.awt.image.BufferedImage(
            800, 600, 
            java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        
        var g = combinedImage.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        g.drawImage(originalImage, 0, 0, null);
        g.drawImage(reconstructionImage, 0, 400, null);
        g.dispose();

        // Save the combined image
        javax.imageio.ImageIO.write(
            combinedImage,
            "png",
            new java.io.File(
                String.format("target/test-output/wavelets/normal/level-%d.png", frequencyLevel)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    public void testTransformAndPlotFrequencyWithChurn(int frequencyLevel) throws IOException {
        Table[] postsAndCustomers = WaveletWithChurnTestSetupSingleton.getPosts();
        Table posts = postsAndCustomers[0];
        Table customers = postsAndCustomers[1];
        
        // Create output directory
        new java.io.File("target/test-output/wavelets/churn").mkdirs();
        
        LocalDate startDate = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN).min();
        LocalDate endDate = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN).max();
        LocalDate churnDate = customers.dateColumn(ProjectConfig.CHURN_DATE_COLUMN).get(0);

        Assertions.assertNotNull(churnDate, "Churn date is null");

        // Group by week and sum post counts, then filter out empty weeks
        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        // Create two separate charts
        XYChart originalChart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Original Weekly Post Counts")
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        XYChart reconstructionChart = new XYChartBuilder()
            .width(800)
            .height(200)
            .title(String.format("Level %d Wavelet Reconstruction", frequencyLevel))
            .xAxisTitle(String.format("Week (from %s to %s)",
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ))
            .yAxisTitle("Amplitude")
            .build();

        // Convert data to arrays for plotting
        double[] weeks = IntStream.range(0, weeklyPosts.rowCount())
            .mapToDouble(i -> i)
            .toArray();
        
        double[] originalPostCounts = weeklyPosts.doubleColumn(summaryColumnName)
            .asDoubleArray();

        // Add the original data series
        originalChart.addSeries(
            "Original Posts", 
            weeks, 
            originalPostCounts
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        // Create a new table with just the summary column for the wavelet transform
        Table transformTable = Table.create("Transform Input")
            .addColumns(
                weeklyPosts.doubleColumn(summaryColumnName)
                    .asIntColumn()
                    .setName(ProjectConfig.POST_COUNT_COLUMN)
            );

        // Add wavelet reconstruction for specified level
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] levelReconstruction = wavelets.transformAndIsolateFrequency(transformTable, frequencyLevel);

        // Truncate the reconstructed data to match the original length
        double[] truncatedReconstruction = Arrays.copyOfRange(
            levelReconstruction, 
            levelReconstruction.length - weeks.length, 
            levelReconstruction.length
        );

        // Add the reconstructed series
        reconstructionChart.addSeries(
            "Reconstruction", 
            weeks, 
            truncatedReconstruction
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        // Add vertical line for churn date in both charts
        int churnYear = churnDate.getYear();
        int churnWeek = churnDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        double churnWeekIndex = churnWeek - 1 + (churnYear - weeklyPosts.intColumn(ProjectConfig.YEAR_COLUMN).min()) * 52;

        // Add churn line to original chart
        double maxOriginalPosts = Math.ceil(posts.intColumn(ProjectConfig.POST_COUNT_COLUMN).max() * 1.1);
        originalChart.addSeries(
            "Churn Date: " + churnDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            new double[]{churnWeekIndex, churnWeekIndex}, 
            new double[]{0, maxOriginalPosts}
        )
            .setLineStyle(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f}, 0.0f))
            .setLineColor(Color.RED)
            .setShowInLegend(true);

        // Add churn line to reconstruction chart
        double maxReconstruction = Arrays.stream(truncatedReconstruction).max().orElse(0) * 1.1;
        reconstructionChart.addSeries(
            "Churn Date",
            new double[]{churnWeekIndex, churnWeekIndex}, 
            new double[]{-maxReconstruction, maxReconstruction}
        )
            .setLineStyle(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f}, 0.0f))
            .setLineColor(Color.RED)
            .setShowInLegend(false);

        // Customize charts
        for (XYChart chart : new XYChart[]{originalChart, reconstructionChart}) {
            chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
            chart.getStyler().setMarkerSize(0);
            chart.getStyler().setPlotGridLinesVisible(false);
        }
        originalChart.getStyler().setYAxisMin(0.0);

        // Create a combined image
        var originalImage = BitmapEncoder.getBufferedImage(originalChart);
        var reconstructionImage = BitmapEncoder.getBufferedImage(reconstructionChart);
        
        var combinedImage = new java.awt.image.BufferedImage(
            800, 600, 
            java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        
        var g = combinedImage.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        g.drawImage(originalImage, 0, 0, null);
        g.drawImage(reconstructionImage, 0, 400, null);
        g.dispose();

        // Save the combined image
        javax.imageio.ImageIO.write(
            combinedImage,
            "png",
            new java.io.File(
                String.format("target/test-output/wavelets/churn/level-%d.png", frequencyLevel)
            )
        );
    }
}
