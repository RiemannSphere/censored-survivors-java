package com.censoredsurvivors.data.statistics;

import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        double[] expectedSignal = {1, 2, 4, 7, 11, 16};

        Assertions.assertEquals(expectedSignal.length, cleanedSignal.length, "The cleaned signal should have the same length as the original signal");
        Assertions.assertArrayEquals(expectedSignal, cleanedSignal, "The cleaned signal should be equal to the expected signal");
    }

    // @Test
    public void testSignalCleaningWithWaveletDenoising() {
        // Create a signal with some noise
        double[] signal = new double[16]; // Using power of 2 length for wavelets
        for (int i = 0; i < signal.length; i++) {
            // Base signal is a simple sine wave
            double baseSignal = 10 * Math.sin(2 * Math.PI * i / 16.0);
            // Add some random noise
            double noise = 2 * (Math.random() - 0.5);
            signal[i] = baseSignal + noise;
        }

        double[] cleanedSignal = SignalCleaner.clean(signal, SignalCleaningType.WAVELET_DENOISING);

        // Verify the cleaned signal has the same length as input
        Assertions.assertEquals(signal.length, cleanedSignal.length, 
            "The cleaned signal should have the same length as the original signal");

        // Calculate mean squared error between original and denoised signal
        double mse = 0;
        for (int i = 0; i < signal.length; i++) {
            mse += Math.pow(signal[i] - cleanedSignal[i], 2);
        }
        mse /= signal.length;

        // The MSE should be relatively small but non-zero (as we're removing noise)
        Assertions.assertTrue(mse > 0 && mse < 10, 
            "MSE should be positive but small, indicating noise removal while preserving signal");

        // Verify that the denoised signal preserves the general shape
        // by checking if the mean absolute difference between consecutive points
        // is smaller in the denoised signal (smoother)
        double originalVariation = 0;
        double denoisedVariation = 0;
        for (int i = 1; i < signal.length; i++) {
            originalVariation += Math.abs(signal[i] - signal[i-1]);
            denoisedVariation += Math.abs(cleanedSignal[i] - cleanedSignal[i-1]);
        }
        
        Assertions.assertTrue(denoisedVariation < originalVariation, 
            "Denoised signal should be smoother than the original signal");
    }

    @Test
    public void testSignalCleaningWithWaveletDenoisingOnSineWave() {
        // Create a signal with some noise
        double[] signal = new double[16]; // Using power of 2 length for wavelets
        for (int i = 0; i < signal.length; i++) {
            // Base signal is a simple sine wave
            double baseSignal = 10 * Math.sin(2 * Math.PI * i / 16.0);
            // Add some random noise
            double noise = 2 * (Math.random() - 0.5);
            signal[i] = baseSignal + noise;
        }

        double[] cleanedSignal = SignalCleaner.clean(signal, SignalCleaningType.WAVELET_DENOISING);

        // Verify the cleaned signal has the same length as input
        Assertions.assertEquals(signal.length, cleanedSignal.length, 
            "The cleaned signal should have the same length as the original signal");

        // Calculate mean squared error between original and denoised signal
        double mse = 0;
        for (int i = 0; i < signal.length; i++) {
            mse += Math.pow(signal[i] - cleanedSignal[i], 2);
        }
        mse /= signal.length;

        // The MSE should be relatively small but non-zero (as we're removing noise)
        Assertions.assertTrue(mse > 0 && mse < 10, 
            "MSE should be positive but small, indicating noise removal while preserving signal");

        // Verify that the denoised signal preserves the general shape
        // by checking if the mean absolute difference between consecutive points
        // is smaller in the denoised signal (smoother)
        double originalVariation = 0;
        double denoisedVariation = 0;
        for (int i = 1; i < signal.length; i++) {
            originalVariation += Math.abs(signal[i] - signal[i-1]);
            denoisedVariation += Math.abs(cleanedSignal[i] - cleanedSignal[i-1]);
        }
        
        Assertions.assertTrue(denoisedVariation < originalVariation, 
            "Denoised signal should be smoother than the original signal");
    }

    @Test
    public void testSignalCleaningWithWaveletDenoisingVisualization() throws IOException {
        // Create a signal with some noise
        double[] signal = new double[16]; // Using power of 2 length for wavelets
        for (int i = 0; i < signal.length; i++) {
            // Generate sine wave with added noise
            double t = i * Math.PI / 8;
            signal[i] = Math.sin(t) + 0.2 * Math.random();
        }

        // Clean the signal
        SignalCleaner cleaner = new SignalCleaner();
        double[] cleanedSignal = cleaner.clean(signal, SignalCleaner.SignalCleaningType.WAVELET_DENOISING);

        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Wavelet Denoising Comparison")
            .xAxisTitle("Sample")
            .yAxisTitle("Amplitude")
            .build();

        // Add original signal series
        double[] xData = IntStream.range(0, signal.length)
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
            "target/test-output/signal-cleaning/wavelet-denoising.png",
            BitmapEncoder.BitmapFormat.PNG
        );
    }
}
