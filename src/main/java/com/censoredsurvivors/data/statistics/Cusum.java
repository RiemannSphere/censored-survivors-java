package com.censoredsurvivors.data.statistics;

import java.util.ArrayList;

public class Cusum {

    /**
     * Smoothing factor for the CUSUM algorithm.
     * 0 means full smoothing, 1 means no smoothing.
     */
    private static final double SMOOTHING = 0.7;

    public record Result(
        double[] cusumValues,
        int anomalyIndex
    ) {}

    /**
     * @see #compute(double[], double, double, boolean)
     */
    public static Result compute(double[] data, double referenceValue, double threshold) {
        return compute(data, referenceValue, threshold, false);
    }

    /**
     * CUSUM (Cumulative Sum) is used to detect small shifts in a process mean.
     *
     * @param data array of data points (x_1, x_2, ..., x_n)
     * @param referenceValue reference value that determines the expected shift in mean.
     * @param threshold value at which to signal a significant change has been detected.
     * @param ignoreZeroValues if true, zero values are ignored in the computation.
     * @return Result containing positive and negative CUSUM values and the first anomaly index (-1 if no anomaly)
     */
    public static Result compute(double[] data, double referenceValue, double threshold, boolean ignoreZeroValues) {
        ArrayList<Double> cusumValues = new ArrayList<>();
        cusumValues.add(0.0);  // Initialize with 0
        int anomalyIndex = -1;
        
        for (int i = 0; i < data.length; i++) {
            if (ignoreZeroValues && data[i] == 0) {
                cusumValues.add(cusumValues.getLast());
                continue;
            }

            double lastCusum = cusumValues.getLast();
            double newCusum = SMOOTHING * (lastCusum + data[i] - referenceValue) + (1 - SMOOTHING) * lastCusum;

            // double newCusum = cusumValues.getLast() + (data[i] - referenceValue);

            cusumValues.add(newCusum);
            
            // Record first crossing of threshold
            if (Math.abs(newCusum) > threshold && anomalyIndex == -1) {
                anomalyIndex = i;
            }
        }

        System.out.println("[END] reference: " + referenceValue + ", threshold: " + threshold);
        
        // Convert ArrayLists to arrays for return value
        return new Result(cusumValues.stream()
            .mapToDouble(Double::doubleValue)
            .toArray(),
            anomalyIndex
        );
    }
}