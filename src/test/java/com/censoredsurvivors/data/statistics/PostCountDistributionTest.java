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
    @Test
    public void testSample() throws IOException {
        double mean = 100;
        double[] stdDevs = {10, 25};
        double[] frequencies = {0.25, 0.1};

        // Create distributions and generate data
        List<List<Double>> allData = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<java.awt.Color> colors = List.of(
            new java.awt.Color(0, 116, 217, 128),    // Blue
            new java.awt.Color(220, 53, 69, 128),    // Red
            new java.awt.Color(40, 167, 69, 128),    // Green
            new java.awt.Color(255, 193, 7, 128)     // Yellow
        );

        int numSamples = 100_000;
        for (double stdDev : stdDevs) {
            for (double frequency : frequencies) {
                PostCountDistribution distribution = new PostCountDistribution(
                    new SocialMediaPostDistributionParams(mean, stdDev, frequency));
                
                List<Double> data = new ArrayList<>();
                for (int i = 0; i < numSamples; i++) {
                    double postCount = distribution.sample();
                    if (postCount > 0) {  // Filter out zero counts
                        data.add(postCount);
                    }
                }
                allData.add(data);
                
                // Create label with parameters and sample size
                String label = String.format(Locale.US, 
                    "Post Count Distribution (μ=%d, σ=%d, f=%.2f) [n=%d]",
                    (int)mean, (int)stdDev, frequency, data.size());
                labels.add(label);
            }
        }

        // Create Chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title("Post Count Distribution Comparison")
            .xAxisTitle("Post Count")
            .yAxisTitle("Frequency")
            .build();

        // Customize Chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setOverlapped(true);
        chart.getStyler().setXAxisLabelRotation(-45);
        chart.getStyler().setDecimalPattern("#");

        // Add all histogram series
        for (int i = 0; i < allData.size(); i++) {
            Histogram histogram = new Histogram(allData.get(i), 48);
            chart.addSeries(labels.get(i), histogram.getxAxisData(), histogram.getyAxisData())
                .setFillColor(colors.get(i));
        }

        // Create output directory if it doesn't exist
        File outputDir = new File("target/test-output");
        outputDir.mkdirs();

        // Save the chart
        BitmapEncoder.saveBitmap(chart, "target/test-output/distribution-comparison", BitmapEncoder.BitmapFormat.PNG);
    }
}
