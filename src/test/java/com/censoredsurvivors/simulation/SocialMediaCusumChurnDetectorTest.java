package com.censoredsurvivors.simulation;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.style.Styler.LegendPosition;

import com.censoredsurvivors.data.statistics.ConfusionStatus;
import com.censoredsurvivors.data.statistics.SignalCleaner;
import com.censoredsurvivors.simulation.SocialMediaCusumChurnDetector.RunSummary;
import com.censoredsurvivors.util.SocialMediaGlobal;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestWord;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class SocialMediaCusumChurnDetectorTest {

    @ParameterizedTest(name = "SignalCleaningType={0}, CusumSmoothing={1}, Threshold={2}")
    @MethodSource("testParameters")
    public void testChurnDetection(
        SignalCleaner.SignalCleaningType signalCleaningType, 
        double cusumSmoothing,
        int threshold
    ) throws IOException {
        int numberOfCustomers = 1_000;
        double churnProbability = 0.5;
        SocialMediaCusumChurnDetector detector = new SocialMediaCusumChurnDetector();
        RunSummary summary = detector.run(numberOfCustomers, churnProbability, cusumSmoothing,1, signalCleaningType);
        
        int total = summary.churnResults().length;
        String truePositive = String.format("%.2f", (double) 100 * summary.numberOfTruePositives() / total);
        String falsePositive = String.format("%.2f", (double) 100 * summary.numberOfFalsePositives() / total);
        String falseNegative = String.format("%.2f", (double) 100 * summary.numberOfFalseNegatives() / total);
        String trueNegative = String.format("%.2f", (double) 100 * summary.numberOfTrueNegatives() / total);

        List<Integer> errors = Arrays.stream(summary.churnResults())
            .filter(result -> result.confusionStatus() == ConfusionStatus.TRUE_POSITIVE)
            .mapToInt(result -> result.detectionErrorInWeeks())
            .map(error -> Math.abs(error))
            .boxed()
            .collect(Collectors.toList());

        double[] errorsArray = errors.stream().mapToDouble(Integer::doubleValue).toArray();
        double medianError = new DescriptiveStatistics(errorsArray).getPercentile(50);

        if (medianError < 7) {
            System.out.println(String.format("[%f, %s, %d] error: %f, false positive: %s", cusumSmoothing, signalCleaningType, threshold, medianError, falsePositive));
        }
        // AsciiTable at = new AsciiTable();
        // at.setTextAlignment(TextAlignment.CENTER);
        // at.getRenderer().setCWC(new CWC_LongestWord());
        // at.addRule();
        // at.addRow("Smoothing: " + cusumSmoothing, "Median Absolute Error: " + medianError + " weeks", "Signal Cleaning: " + signalCleaningType);
        // at.addRule();
        // at.addRow("# Customers", "True Positive", "False Positive");
        // at.addRow(numberOfCustomers, truePositive + "%", falsePositive + "%");
        // at.addRule();
        // at.addRow("Churn Probability", "False Negative", "True Negative");
        // at.addRow(churnProbability, falseNegative + "%", trueNegative + "%");
        // at.addRule();
        // System.out.println(at.render());

        //printHistogram(errors, "Detection Error Distribution", "Detection Error (weeks)");
    }

    private static Stream<Arguments> testParameters() {
        return Arrays.stream(SignalCleaner.SignalCleaningType.values())
                .flatMap(cleaningType -> 
                    Stream.of(0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4)
                        .flatMap(smoothing ->
                            Stream.of(1, 2, 3, 4, 5)
                                .map(threshold -> Arguments.of(cleaningType, smoothing, threshold))
                        )
                );
    }

    private void printHistogram(List<Integer> data, String title, String xAxisLabel) throws IOException {
        // Create histogram
        int minValue = data.stream().mapToInt(d -> d).min().orElse(0);
        int maxValue = data.stream().mapToInt(d -> d).max().orElse(0);
        int binCount = (int) Math.ceil(1 + 3.322 * Math.log10(data.size()));
        Histogram histogram = new Histogram(
            data,
            binCount,
            minValue,
            maxValue
        );

        // Create chart
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title(title)
            .xAxisTitle(xAxisLabel)
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
            String.format(Locale.US, "Detection Errors [n=%d]", data.size()),
            histogram.getxAxisData(),
            histogram.getyAxisData()
        ).setFillColor(new Color(0, 116, 217, 128));  // Blue

        // Create output directory if it doesn't exist
        new File("target/test-output").mkdirs();

        // Save chart
        BitmapEncoder.saveBitmap(
            chart,
            "target/test-output/detection-errors-histogram.png",
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
