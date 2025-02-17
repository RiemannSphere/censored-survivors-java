package com.censoredsurvivors.data.statistics;

import java.util.ArrayList;

public class Cusum {
    public record Result(
        double[] cusumValues,
        int anomalyIndex
    ) {}

    private double smoothing;

    /**
     * @param smoothing the smoothing factor of the CUSUM algorithm.
     * 0 means full smoothing, 1 means no smoothing.
     */
    public Cusum(double smoothing) {
        this.smoothing = smoothing;
    }

    /**
     * @see #compute(double[], double, double, boolean)
     */
    public Result compute(double[] data, double referenceValue, double threshold) {
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
    public Result compute(double[] data, double referenceValue, double threshold, boolean ignoreZeroValues) {
        ArrayList<Double> cusumValues = new ArrayList<>();
        int anomalyIndex = -1;
        
        for (int i = 0; i < data.length; i++) {
            double lastCusum = cusumValues.isEmpty() 
                ? 0.0 
                : cusumValues.get(cusumValues.size() - 1);

            if (ignoreZeroValues && data[i] == 0) {
                cusumValues.add(lastCusum);
                continue;
            }

            double newCusum = smoothing * (lastCusum + data[i] - referenceValue) + (1 - smoothing) * lastCusum;

            // double newCusum = lastCusum + (data[i] - referenceValue);

            cusumValues.add(newCusum);
            
            // Record first crossing of threshold
            if (Math.abs(newCusum) > threshold && anomalyIndex == -1) {
                anomalyIndex = i;
            }
        }
        
        // Convert ArrayLists to arrays for return value
        return new Result(cusumValues.stream()
            .mapToDouble(Double::doubleValue)
            .toArray(),
            anomalyIndex
        );
    }
}