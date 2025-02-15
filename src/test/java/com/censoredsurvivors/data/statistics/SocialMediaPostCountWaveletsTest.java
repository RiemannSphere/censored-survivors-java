package com.censoredsurvivors.data.statistics;

import java.io.IOException;
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

class WaveletTestSetupSingleton {
    private static final boolean ALL_CUSTOMERS_FULL_LIFETIME = true; // ensure enough posts are generated
    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final double PROPORTION_OF_LEFT_CENSORED_CUSTOMERS = 0.1;
    private static final double PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS = 0.1;
    private static final int NUMBER_OF_YEARS = 5;

    private static Table posts;

    public static Table getPosts() {
        if (posts == null) {
            SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator(ALL_CUSTOMERS_FULL_LIFETIME);
            Table customers = customerGenerator.generateCustomers(NUMBER_OF_CUSTOMERS, PROPORTION_OF_LEFT_CENSORED_CUSTOMERS, PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS, NUMBER_OF_YEARS);
            SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
            List<SocialMediaPostRule> postRules = List.of(
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
                new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.name(), new SocialMediaPostDistributionParams(100, 50, 0.5))
            );
            List<SocialMediaChannel> channels = List.of(SocialMediaChannel.FACEBOOK, SocialMediaChannel.INSTAGRAM);
    
            posts = postsGenerator.generatePosts("Platform Posts", postRules, channels);
        }

        return posts;
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
                String.format("target/test-output/wavelet-analysis-level-%d.png", frequencyLevel)
            )
        );
    }
}
