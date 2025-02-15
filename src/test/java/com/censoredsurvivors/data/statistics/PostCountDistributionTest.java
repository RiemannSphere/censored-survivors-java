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
        List<List<Double>> allData = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Sample from first distribution
        List<Double> data1 = java.util.stream.Stream.generate(dist1::sample)
            .limit(NUM_SAMPLES)
            .filter(postCount -> postCount > 0)
            .map(Double::valueOf)
            .collect(java.util.stream.Collectors.toList());
        allData.add(data1);
        labels.add(String.format(Locale.US,
            "Distribution 1 (μ=%d, σ=%d, f=%.2f) [n=%d]",
            (int)params1.mean(), (int)params1.stdDev(), params1.frequency(), data1.size()));

        // Sample from second distribution
        List<Double> data2 = java.util.stream.Stream.generate(dist2::sample)
            .limit(NUM_SAMPLES)
            .filter(postCount -> postCount > 0)
            .map(Double::valueOf)
            .collect(java.util.stream.Collectors.toList());
        allData.add(data2);
        labels.add(String.format(Locale.US,
            "Distribution 2 (μ=%d, σ=%d, f=%.2f) [n=%d]",
            (int)params2.mean(), (int)params2.stdDev(), params2.frequency(), data2.size()));

        // Create Chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title(title)
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize Chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setOverlapped(true);
        chart.getStyler().setXAxisLabelRotation(-45);
        chart.getStyler().setDecimalPattern("#");

        // Add histogram series
        for (int i = 0; i < allData.size(); i++) {
            Histogram histogram = new Histogram(allData.get(i), NUM_HISTOGRAM_BINS);
            chart.addSeries(labels.get(i), histogram.getxAxisData(), histogram.getyAxisData())
                .setFillColor(COLORS.get(i));
        }

        // Save the chart
        BitmapEncoder.saveBitmap(chart, "target/test-output/" + outputFileName, BitmapEncoder.BitmapFormat.PNG);
    }
}
