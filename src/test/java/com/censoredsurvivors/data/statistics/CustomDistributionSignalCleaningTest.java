package com.censoredsurvivors.data.statistics;

import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.censoredsurvivors.data.model.CustomDistributionParams;
import com.censoredsurvivors.data.statistics.SignalCleaner.SignalCleaningType;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.BitmapEncoder;

public class CustomDistributionSignalCleaningTest {
    @Test
    public void testSignalCleaning() {
        double[] signal = {1, 2, 3, 4, 5};
        double[] cleanedSignal = SignalCleaner.clean(signal, SignalCleaningType.NONE);

        Assertions.assertArrayEquals(signal, cleanedSignal);
    }

    @Test
    public void testSignalCleaningWithMovingAverage() {
        // 5 ones as the window size is 5
        double[] signal = {1, 1, 1, 1, 1, 6, 11, 16, 21, 26};

        double[] cleanedSignal = SignalCleaner.clean(signal, SignalCleaningType.SIMPLE_MOVING_AVERAGE);
        double[] expectedSignal = {1, 1, 1, 1, 1, 2, 4, 7, 11, 16};

        Assertions.assertEquals(expectedSignal.length, cleanedSignal.length, "The cleaned signal should have the same length as the original signal");
        Assertions.assertArrayEquals(expectedSignal, cleanedSignal, "The cleaned signal should be equal to the expected signal");
    }

    @ParameterizedTest
    @EnumSource(SignalCleaner.SignalCleaningType.class)
    public void testhWaveletDenoisingVisualizationSineWave(SignalCleaner.SignalCleaningType signalCleaningType) throws IOException {
        // Create a signal with some noise
        double[] signal = new double[128]; // Using power of 2 length for wavelets
        for (int i = 0; i < signal.length; i++) {
            // Base signal is a simple sine wave
            double baseSignal = 10 * Math.sin(2 * Math.PI * i / 16.0);
            // Add some random noise
            double noise = 4 * (Math.random() - 0.5);
            signal[i] = baseSignal + noise;
        }

        double[] cleanedSignal = SignalCleaner.clean(signal, signalCleaningType);

        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Wavelet Denoising Comparison")
            .xAxisTitle("Sample")
            .yAxisTitle("Amplitude")
            .build();

        // Add original signal series
        double[] xData = IntStream.range(0, cleanedSignal.length)
            .mapToDouble(i -> i)
            .toArray();

        chart.addSeries("Original Signal", xData, signal)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.BLUE);

        // Add cleaned signal series
        chart.addSeries("Denoised Signal", xData, cleanedSignal)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.RED);

        // Style chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);

        // Create output directory if it doesn't exist
        new File("target/test-output/signal-cleaning").mkdirs();

        // Save chart
        BitmapEncoder.saveBitmap(
            chart,
            "target/test-output/signal-cleaning/sine-wave-" + signalCleaningType.name() + ".png",
            BitmapEncoder.BitmapFormat.PNG
        );
    }

    @ParameterizedTest
    @EnumSource(SignalCleaner.SignalCleaningType.class)
    public void testhWaveletDenoisingVisualizationCustomDistribution(SignalCleaner.SignalCleaningType signalCleaningType) throws IOException {
        CustomDistributionParams params = new CustomDistributionParams(100, 10, 0.8);
        CustomDistribution customDistribution = new CustomDistribution(params);
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = customDistribution.sample();
        }

        double[] cleanedSignal = SignalCleaner.clean(signal, signalCleaningType);
        
        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Wavelet Denoising Comparison")
            .xAxisTitle("Sample")
            .yAxisTitle("Amplitude")
            .build();

        // Add original signal series
        double[] xData = IntStream.range(0, cleanedSignal.length)
            .mapToDouble(i -> i)
            .toArray();

        chart.addSeries("Original Signal", xData, signal)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.BLUE);

        // Add cleaned signal series
        chart.addSeries("Denoised Signal", xData, cleanedSignal)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
            .setLineColor(Color.RED);

        // Style chart
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        
        // Create output directory if it doesn't exist
        new File("target/test-output/signal-cleaning").mkdirs();

        // Save chart
        BitmapEncoder.saveBitmap(
            chart,
            "target/test-output/signal-cleaning/custom-distribution-" + signalCleaningType.name() + ".png",
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
