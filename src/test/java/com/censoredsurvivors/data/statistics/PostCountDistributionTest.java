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

public class PostCountDistributionTest {
    @Test
    public void testSample() throws IOException {
        // Create distributions
        PostCountDistribution postCountDistribution = new PostCountDistribution(
            new SocialMediaPostDistributionParams(100, 10, 1));
        NormalDistribution normalDistribution = new NormalDistribution(100, 10);

        // Generate data points
        List<Double> postCountData = new ArrayList<>();
        List<Double> normalData = new ArrayList<>();

        // Sample points from both distributions
        int numSamples = 100_000;
        for (int i = 0; i < numSamples; i++) {
            postCountData.add((double) postCountDistribution.sample());
            normalData.add(normalDistribution.sample());
        }

        Histogram postCountHistogram = new Histogram(postCountData, 12);
        Histogram normalHistogram = new Histogram(normalData, 12);

        // Create Chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title("Post Count Distribution")
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize Chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setOverlapped(true);
        chart.getStyler().setXAxisLabelRotation(-45);
        chart.getStyler().setDecimalPattern("#");

        // Add histogram series
        chart.addSeries("Post Count Distribution", postCountHistogram.getxAxisData(), postCountHistogram.getyAxisData())
            .setFillColor(new java.awt.Color(0, 116, 217, 128));  // Semi-transparent blue
        chart.addSeries("Normal Distribution", normalHistogram.getxAxisData(), normalHistogram.getyAxisData())
            .setFillColor(new java.awt.Color(220, 53, 69, 128));  // Semi-transparent red

        // Create output directory if it doesn't exist
        File outputDir = new File("target/test-output");
        outputDir.mkdirs();

        // Save the chart
        BitmapEncoder.saveBitmap(chart, "target/test-output/distribution-comparison", BitmapEncoder.BitmapFormat.PNG);
    }
}
