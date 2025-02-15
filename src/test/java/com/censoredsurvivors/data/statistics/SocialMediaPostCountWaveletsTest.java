package com.censoredsurvivors.data.statistics;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
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
import org.knowm.xchart.SwingWrapper;

@TestInstance(Lifecycle.PER_CLASS)
public class SocialMediaPostCountWaveletsTest {
    private static final int NUMBER_OF_CUSTOMERS = 1;
    private static final double PROPORTION_OF_LEFT_CENSORED_CUSTOMERS = 0.1;
    private static final double PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS = 0.1;
    private static final int NUMBER_OF_YEARS = 10;

    private Table posts;

    @BeforeAll
    public void setUp() {
        SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator();
        Table customers = customerGenerator.generateCustomers(NUMBER_OF_CUSTOMERS, PROPORTION_OF_LEFT_CENSORED_CUSTOMERS, PROPORTION_OF_RIGHT_CENSORED_CUSTOMERS, NUMBER_OF_YEARS);
        SocialMediaPostsGenerator postsGenerator = new SocialMediaPostsGenerator(customers);
        List<SocialMediaPostRule> postRules = List.of(
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.FACEBOOK.name(), new SocialMediaPostDistributionParams(200, 20, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.INSTAGRAM.name(), new SocialMediaPostDistributionParams(100, 50, 0.5)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.TWITTER.name(), new SocialMediaPostDistributionParams(10, 1, 0.8)),
            new SocialMediaPostRule(SocialMediaParam.CHANNEL, SocialMediaChannel.LINKEDIN.name(), new SocialMediaPostDistributionParams(5, 2, 0.25))
        );
        posts = postsGenerator.generatePosts("Platform Posts", postRules);
    }

    @Test
    public void testTransform() {
        SocialMediaPostCountWavelets wavelets = new SocialMediaPostCountWavelets();
        double[] coefficients = wavelets.transform(posts);

        System.out.println(Arrays.toString(coefficients));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void testTransformAndPlotFrequency(int frequencyLevel) throws IOException {
        // Group by week and sum post counts
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
        
        String summaryColumnName = "Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]";
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
