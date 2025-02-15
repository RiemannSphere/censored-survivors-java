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
            new SocialMediaPostDistributionParams(150, 10, 1.0),
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

        // Create histograms with shared bins
        double minValue = Math.min(
            data1.stream().mapToDouble(d -> d).min().orElse(0),
            data2.stream().mapToDouble(d -> d).min().orElse(0)
        );
        double maxValue = Math.max(
            data1.stream().mapToDouble(d -> d).max().orElse(0),
            data2.stream().mapToDouble(d -> d).max().orElse(0)
        );
        
        // Create histograms with the same bin boundaries
        Histogram histogram1 = new Histogram(data1, NUM_HISTOGRAM_BINS, minValue, maxValue);
        Histogram histogram2 = new Histogram(data2, NUM_HISTOGRAM_BINS, minValue, maxValue);

        // Calculate shared Y axis max
        double maxY = Math.max(
            histogram1.getyAxisData().stream().mapToDouble(d -> d).max().orElse(0),
            histogram2.getyAxisData().stream().mapToDouble(d -> d).max().orElse(0)
        );

        // Create single chart for both distributions
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)  // Full height since we're using one chart
            .title(title + " - Post Count Distributions")
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize Chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setOverlapped(true);
        chart.getStyler().setXAxisLabelRotation(-45);
        chart.getStyler().setDecimalPattern("#");
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(maxY);

        System.out.println("Histogram 1: " + histogram1.getxAxisData().stream().mapToInt(x -> x.intValue()).average());
        System.out.println("Histogram 2: " + histogram2.getxAxisData().stream().mapToInt(x -> x.intValue()).average());

        // Add both histogram series to the same chart
        chart.addSeries(
            String.format(Locale.US, "μ=%d, σ=%d, f=%.2f [n=%d]",
                (int)params1.mean(), (int)params1.stdDev(), params1.frequency(), data1.size()),
            histogram1.getxAxisData(),
            histogram1.getyAxisData()
        ).setFillColor(COLORS.get(0));

        chart.addSeries(
            String.format(Locale.US, "μ=%d, σ=%d, f=%.2f [n=%d]",
                (int)params2.mean(), (int)params2.stdDev(), params2.frequency(), data2.size()),
            histogram2.getxAxisData(),
            histogram2.getyAxisData()
        ).setFillColor(COLORS.get(1));

        // Save the single chart
        BitmapEncoder.saveBitmap(
            chart, 
            "target/test-output/" + outputFileName + ".png",
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
