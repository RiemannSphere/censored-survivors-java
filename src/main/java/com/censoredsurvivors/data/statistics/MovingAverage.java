package com.censoredsurvivors.data.statistics;

public class MovingAverage {

    /**
     * Computes the simple moving average of the given data array.
     * 
     * @param data the data array to compute the moving average of
     * @param window the window size
     * @return the moving average of the data
     * @throws IllegalArgumentException if the data array is null, the window size is not positive, or the data length is less than the window size
     */
    public static double[] simpleMovingAverage(double[] data, int window) {
        if (data == null) {
            throw new IllegalArgumentException("Data array cannot be null");
        }
        if (window <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (data.length < window) {
            throw new IllegalArgumentException("Data length must be at least as long as the window size");
        }

        double[] result = new double[data.length];
        
        // Handle the beginning (with smaller windows)
        for (int i = 0; i < window - 1; i++) {
            double sum = 0;
            for (int j = 0; j <= i; j++) {
                sum += data[j];
            }
            result[i] = sum / (i + 1);
        }

        // Handle the middle (with full window)
        double sum = 0;
        for (int i = 0; i < window; i++) {
            sum += data[i];
        }
        result[window - 1] = sum / window;

        // Slide the window over the rest of the data
        for (int i = window; i < data.length; i++) {
            sum += data[i] - data[i - window];
            result[i] = sum / window;
        }

        return result;
    }
}
