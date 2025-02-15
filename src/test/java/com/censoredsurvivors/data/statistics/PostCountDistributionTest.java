package com.censoredsurvivors.data.statistics;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.style.Styler.LegendPosition;

import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostCountDistributionTest {
    private static final int NUM_SAMPLES = 100_000;
    private static final int NUM_HISTOGRAM_BINS = 48;
    private static final List<java.awt.Color> COLORS = List.of(
        new java.awt.Color(220, 53, 69, 128),     // Red
        new java.awt.Color(0, 116, 217, 128)      // Blue
    );

    @Test
    public void testSample() throws IOException {
        // Create output directory if it doesn't exist
        File outputDir = new File("target/test-output");
        outputDir.mkdirs();

        // 1. Compare different frequencies
        compareDistributions(
            "Frequency Comparison",
            new SocialMediaPostDistributionParams(100, 10, 0.25),
            new SocialMediaPostDistributionParams(100, 10, 1.0),
            "frequency-comparison"
        );

        // 2. Compare different standard deviations
        compareDistributions(
            "Standard Deviation Comparison",
            new SocialMediaPostDistributionParams(100, 10, 1.0),
            new SocialMediaPostDistributionParams(100, 25, 1.0),
            "stddev-comparison"
        );

        // 3. Compare different means
        compareDistributions(
            "Mean Comparison",
            new SocialMediaPostDistributionParams(250, 10, 1.0),
            new SocialMediaPostDistributionParams(100, 10, 1.0),
            "mean-comparison"
        );
    }

    private void compareDistributions(
        String title,
        SocialMediaPostDistributionParams params1,
        SocialMediaPostDistributionParams params2,
        String outputFileName
    ) throws IOException {
        
        // Create distributions
        PostCountDistribution dist1 = new PostCountDistribution(params1);
        PostCountDistribution dist2 = new PostCountDistribution(params2);

        // Generate data
        List<Double> data1 = java.util.stream.Stream.generate(dist1::sample)
            .limit(NUM_SAMPLES)
            .filter(postCount -> postCount > 0)
            .map(Double::valueOf)
            .collect(java.util.stream.Collectors.toList());

        List<Double> data2 = java.util.stream.Stream.generate(dist2::sample)
            .limit(NUM_SAMPLES)
            .filter(postCount -> postCount > 0)
            .map(Double::valueOf)
            .collect(java.util.stream.Collectors.toList());

        // Create histograms
        Histogram histogram1 = new Histogram(data1, NUM_HISTOGRAM_BINS);
        Histogram histogram2 = new Histogram(data2, NUM_HISTOGRAM_BINS);

        // Create two separate charts
        CategoryChart chart1 = new CategoryChartBuilder()
            .width(800)
            .height(300)  // Half the original height
            .title(title + " - Post Count Distributions")
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        CategoryChart chart2 = new CategoryChartBuilder()
            .width(800)
            .height(300)  // Half the original height
            .title("")  // Empty title for second chart since we have a combined title
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize Charts
        for (CategoryChart chart : List.of(chart1, chart2)) {
            chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
            chart.getStyler().setAvailableSpaceFill(0.99);
            chart.getStyler().setXAxisLabelRotation(-45);
            chart.getStyler().setDecimalPattern("#");
        }

        System.out.println("Histogram 1: " + histogram1.getxAxisData().stream().mapToInt(x -> x.intValue()).average());
        System.out.println("Histogram 2: " + histogram2.getxAxisData().stream().mapToInt(x -> x.intValue()).average());

        // Add histogram series to separate charts
        chart1.addSeries(
            String.format(Locale.US, "μ=%d, σ=%d, f=%.2f [n=%d]",
                (int)params1.mean(), (int)params1.stdDev(), params1.frequency(), data1.size()),
            histogram1.getxAxisData(),
            histogram1.getyAxisData()
        ).setFillColor(COLORS.get(0));

        chart2.addSeries(
            String.format(Locale.US, "μ=%d, σ=%d, f=%.2f [n=%d]",
                (int)params2.mean(), (int)params2.stdDev(), params2.frequency(), data2.size()),
            histogram2.getxAxisData(),
            histogram2.getyAxisData()
        ).setFillColor(COLORS.get(1));

        // Create a combined vertical image
        java.awt.image.BufferedImage chart1Image = BitmapEncoder.getBufferedImage(chart1);
        java.awt.image.BufferedImage chart2Image = BitmapEncoder.getBufferedImage(chart2);
        
        java.awt.image.BufferedImage combined = new java.awt.image.BufferedImage(
            800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g2d = combined.createGraphics();
        g2d.drawImage(chart1Image, 0, 0, null);
        g2d.drawImage(chart2Image, 0, 300, null);
        g2d.dispose();

        // Save the combined image
        javax.imageio.ImageIO.write(
            combined, 
            "PNG", 
            new File("target/test-output/" + outputFileName + ".png")
        );
    }
}
