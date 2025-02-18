package com.censoredsurvivors.data.statistics;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    private record ChartData(
        double[] weeks,
        double[] originalPostCounts,
        double[] reconstructedCounts,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate churnDate
    ) {}

    @Test
    public void testTransform() {
        Table posts = WaveletTestSetupSingleton.getPosts();

        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        double[] postCounts = weeklyPosts.doubleColumn("Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]")
            .asDoubleArray();

        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] coefficients = wavelets.transform(postCounts);

        // Calculate the next power of 2 after posts.rowCount()
        int nextPowerOfTwo = 1 << (32 - Integer.numberOfLeadingZeros(postCounts.length - 1));
        Assertions.assertEquals(nextPowerOfTwo, coefficients.length);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    public void testTransformAndPlotFrequency(int frequencyLevel) throws IOException {
        Table posts = WaveletTestSetupSingleton.getPosts();
        
        ChartData chartData = prepareChartData(posts, null, frequencyLevel);
        XYChart[] charts = createCharts(chartData, frequencyLevel, false);
        saveCharts(charts, String.format("target/test-output/wavelets/normal/level-%d.png", frequencyLevel));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    public void testTransformAndPlotFrequencyWithChurn(int frequencyLevel) throws IOException {
        Table[] postsAndCustomers = WaveletWithChurnTestSetupSingleton.getPosts();
        Table posts = postsAndCustomers[0];
        Table customers = postsAndCustomers[1];
        
        ChartData chartData = prepareChartData(posts, customers, frequencyLevel);
        XYChart[] charts = createCharts(chartData, frequencyLevel, true);
        saveCharts(charts, String.format("target/test-output/wavelets/churn/level-%d.png", frequencyLevel));
    }

    private ChartData prepareChartData(Table posts, Table customers, int frequencyLevel) {
        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
        
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        double[] weeks = IntStream.range(0, weeklyPosts.rowCount())
            .mapToDouble(i -> i)
            .toArray();
        
        double[] postCounts = weeklyPosts.doubleColumn(summaryColumnName)
            .asDoubleArray();

        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] levelReconstruction = wavelets.reconstructByFrequency(postCounts, frequencyLevel);
        double[] truncatedReconstruction = Arrays.copyOfRange(
            levelReconstruction, 
            levelReconstruction.length - weeks.length, 
            levelReconstruction.length
        );

        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDate churnDate = null;

        if (customers != null) {
            startDate = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN).min();
            endDate = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN).max();
            churnDate = customers.dateColumn(ProjectConfig.CHURN_DATE_COLUMN).get(0);
        }

        return new ChartData(
            weeks,
            postCounts,
            truncatedReconstruction,
            startDate,
            endDate,
            churnDate
        );
    }

    private XYChart[] createCharts(ChartData data, int frequencyLevel, boolean includeChurn) {
        XYChart originalChart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Original Weekly Post Counts")
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        String xAxisTitle = "Week";
        if (data.startDate != null && data.endDate != null) {
            xAxisTitle = String.format("Week (from %s to %s)",
                data.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                data.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }

        XYChart reconstructionChart = new XYChartBuilder()
            .width(800)
            .height(200)
            .title(String.format("Level %d Wavelet Reconstruction", frequencyLevel))
            .xAxisTitle(xAxisTitle)
            .yAxisTitle("Amplitude")
            .build();

        // Add data series
        originalChart.addSeries(
            "Original Posts", 
            data.weeks, 
            data.originalPostCounts
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        reconstructionChart.addSeries(
            "Reconstruction", 
            data.weeks, 
            data.reconstructedCounts
        ).setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);

        if (includeChurn && data.churnDate != null) {
            addChurnLines(originalChart, reconstructionChart, data);
        }

        // Style charts
        for (XYChart chart : new XYChart[]{originalChart, reconstructionChart}) {
            chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
            chart.getStyler().setMarkerSize(0);
            chart.getStyler().setPlotGridLinesVisible(false);
            chart.getStyler().setPlotGridVerticalLinesVisible(false);
        }

        return new XYChart[]{originalChart, reconstructionChart};
    }

    private void addChurnLines(XYChart originalChart, XYChart reconstructionChart, ChartData data) {
        int churnYear = data.churnDate.getYear();
        int churnWeek = data.churnDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        int startYear = data.startDate.getYear();
        
        // Calculate week index relative to start date
        double churnWeekIndex = (churnYear - startYear) * 52 + churnWeek - data.startDate.get(WeekFields.ISO.weekOfWeekBasedYear());

        double maxOriginalPosts = Math.ceil(Arrays.stream(data.originalPostCounts).max().orElse(0) * 1.1);
        originalChart.addSeries(
            "Churn Date: " + data.churnDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            new double[]{churnWeekIndex, churnWeekIndex}, 
            new double[]{0, maxOriginalPosts}
        )
            .setLineStyle(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f}, 0.0f))
            .setLineColor(Color.RED)
            .setShowInLegend(true);

        double maxReconstruction = Arrays.stream(data.reconstructedCounts).max().orElse(0) * 1.1;
        double minReconstruction = Arrays.stream(data.reconstructedCounts).min().orElse(0) * 1.1;
        reconstructionChart.addSeries(
            "Churn Date",
            new double[]{churnWeekIndex, churnWeekIndex}, 
            new double[]{minReconstruction, maxReconstruction}
        )
            .setLineStyle(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f}, 0.0f))
            .setLineColor(Color.RED)
            .setShowInLegend(false);
    }

    private void saveCharts(XYChart[] charts, String outputPath) throws IOException {
        var originalImage = BitmapEncoder.getBufferedImage(charts[0]);
        var reconstructionImage = BitmapEncoder.getBufferedImage(charts[1]);
        
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

        // Create output directory
        new java.io.File(outputPath).getParentFile().mkdirs();

        // Save the combined image
        javax.imageio.ImageIO.write(
            combinedImage,
            "png",
            new java.io.File(outputPath)
        );
    }

    private record FrequencyBandTestCase(
        String name,
        int[] levels,
        String description
    ) {}

    private static Stream<FrequencyBandTestCase> frequencyBandTestCases() {
        return Stream.of(
            new FrequencyBandTestCase(
                "low_pass",
                new int[]{0, 1, 2},
                "Low frequency components (trend)"
            ),
            new FrequencyBandTestCase(
                "high_pass",
                new int[]{6, 7, 8},
                "High frequency components (noise)"
            ),
            new FrequencyBandTestCase(
                "mid_pass",
                new int[]{3, 4, 5},
                "Mid frequency components (seasonality)"
            ),
            new FrequencyBandTestCase(
                "all_pass",
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8},
                "All frequency components"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("frequencyBandTestCases")
    public void testReconstructByFrequencies(FrequencyBandTestCase testCase) throws IOException {
        Table posts = WaveletTestSetupSingleton.getPosts();

        // Prepare weekly aggregated data
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
        double[] postCounts = weeklyPosts.doubleColumn(summaryColumnName)
            .asDoubleArray();

        double[] weeks = IntStream.range(0, postCounts.length)
            .mapToDouble(i -> i)
            .toArray();

        // Transform and reconstruct
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] reconstructed = wavelets.reconstructByFrequencies(postCounts, testCase.levels);

        // remove padding on front 
        double[] reconstructedWithoutPadding = 
            Arrays.copyOfRange(reconstructed, reconstructed.length - postCounts.length, reconstructed.length);

        IntStream
            .range(0, Math.min(reconstructedWithoutPadding.length, postCounts.length))
            .mapToObj(i -> testCase.name + ":" + reconstructedWithoutPadding[i] + ":" + postCounts[i])
            .forEach(System.out::println);

        Assertions.assertEquals(reconstructedWithoutPadding.length, postCounts.length, "Reconstructed and post counts should have the same length");

        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title(String.format("Frequency Band Reconstruction (%s)", testCase.description))
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        // Add original data series
        chart.addSeries(
            "Original Posts",
            weeks,
            postCounts
        )
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.BLUE);

        // Add reconstructed data series
        chart.addSeries(
            String.format("Reconstructed (levels %s)", Arrays.toString(testCase.levels)),
            weeks,
            reconstructedWithoutPadding
        )
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.RED);

        // Style chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);

        // Save chart
        new java.io.File("target/test-output/wavelets/frequency_bands").mkdirs();
        BitmapEncoder.saveBitmap(
            chart,
            String.format("target/test-output/wavelets/frequency_bands/%s.png", testCase.name),
            BitmapEncoder.BitmapFormat.PNG
        );

        // Verify reconstruction properties
        Assertions.assertEquals(postCounts.length, reconstructed.length - (reconstructed.length - postCounts.length));
        
        // For low pass, verify that high frequency components are removed (signal is smoother)
        if (testCase.name.equals("low_pass")) {
            double originalVariance = calculateVariance(postCounts);
            double reconstructedVariance = calculateVariance(
                Arrays.copyOfRange(reconstructed, reconstructed.length - postCounts.length, reconstructed.length)
            );
            Assertions.assertTrue(reconstructedVariance < originalVariance, 
                "Low pass reconstruction should have lower variance than original signal");
        }
    }

    private double calculateVariance(double[] data) {
        double mean = Arrays.stream(data).average().orElse(0.0);
        return Arrays.stream(data)
            .map(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);
    }

    @Test
    public void testIdentityTransform() throws IOException {
        Table posts = WaveletTestSetupSingleton.getPosts();

        // Prepare weekly aggregated data
        Table weeklyPosts = posts.summarize(
            ProjectConfig.POST_COUNT_COLUMN, 
            tech.tablesaw.aggregate.AggregateFunctions.sum
        ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
        double[] postCounts = weeklyPosts.doubleColumn(summaryColumnName)
            .asDoubleArray();

        double[] weeks = IntStream.range(0, postCounts.length)
            .mapToDouble(i -> i)
            .toArray();

        // Transform and reconstruct
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] reconstructed = wavelets.identityTransform(postCounts);

        // remove padding on front 
        double[] reconstructedWithoutPadding = 
            Arrays.copyOfRange(reconstructed, reconstructed.length - postCounts.length, reconstructed.length);

        // Print values for debugging
        IntStream
            .range(0, Math.min(reconstructedWithoutPadding.length, postCounts.length))
            .mapToObj(i -> String.format("Week %d: Original=%.2f, Reconstructed=%.2f", 
                i, postCounts[i], reconstructedWithoutPadding[i]))
            .forEach(System.out::println);

        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Identity Transform Reconstruction")
            .xAxisTitle("Week")
            .yAxisTitle("Number of Posts")
            .build();

        // Add original data series
        chart.addSeries(
            "Original Posts",
            weeks,
            postCounts
        )
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.BLUE);

        // Add reconstructed data series
        chart.addSeries(
            "Reconstructed",
            weeks,
            reconstructedWithoutPadding
        )
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.RED);

        // Style chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);

        // Save chart
        new java.io.File("target/test-output/wavelets/identity").mkdirs();
        BitmapEncoder.saveBitmap(
            chart,
            "target/test-output/wavelets/identity/reconstruction.png",
            BitmapEncoder.BitmapFormat.PNG
        );

        // Verify reconstruction properties
        Assertions.assertEquals(postCounts.length, reconstructedWithoutPadding.length, 
            "Original and reconstructed signals should have the same length");
        
        // Check if reconstruction is close to original (allowing for small numerical errors)
        double maxError = 1e-10;
        for (int i = 0; i < postCounts.length; i++) {
            Assertions.assertEquals(postCounts[i], reconstructedWithoutPadding[i], maxError,
                String.format("Reconstruction error at week %d exceeds tolerance", i));
        }
    }
}
