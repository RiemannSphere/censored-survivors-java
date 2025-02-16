package com.censoredsurvivors.data.statistics;

import java.util.ArrayList;

public class Cusum {

    public record Result(
        double[] positiveCusumValues,
        double[] negativeCusumValues,
        int anomalyIndex
    ) {}

    /**
     * Computes both positive and negative CUSUM values in a single pass.
     * CUSUM (Cumulative Sum) is used to detect small shifts in a process mean.
     *
     * @param data          array of data points (x_1, x_2, ..., x_n)
     * @param referenceValue reference value that determines the expected shift in mean.
     *                     Higher values make the algorithm less sensitive to changes.
     * @param threshold     value at which to signal a significant change has been detected.
     *                     When CUSUM exceeds this value, it indicates an out-of-control condition.
     * @return             Result containing positive and negative CUSUM values and the first anomaly index (-1 if no anomaly)
     */
    public static Result computeCusum(double[] data, double referenceValue, double threshold) {
        ArrayList<Double> positiveCusumValues = new ArrayList<>();
        ArrayList<Double> negativeCusumValues = new ArrayList<>();
        positiveCusumValues.add(0.0);  // Initialize with 0
        negativeCusumValues.add(0.0);  // Initialize with 0
        int anomalyIndex = -1;
        
        for (int i = 0; i < data.length; i++) {
            // Calculate new positive CUSUM value: S^+_{n+1} = max(0, S^+_n + x - K)
            double newPositiveCusum = Math.max(0.0, positiveCusumValues.getLast() + data[i] - referenceValue);
            // Calculate new negative CUSUM value: S^-_{n+1} = min(0, S^-_n + x + K)
            double newNegativeCusum = Math.min(0.0, negativeCusumValues.getLast() + data[i] - referenceValue);
            
            positiveCusumValues.add(newPositiveCusum);
            negativeCusumValues.add(newNegativeCusum);
            
            // Record first crossing of threshold for either positive or negative CUSUM
            if ((newPositiveCusum > threshold || Math.abs(newNegativeCusum) > threshold) && anomalyIndex == -1) {
                anomalyIndex = i;
            }
        }
        
        // Convert ArrayLists to arrays for return value
        double[] positiveCusumArray = positiveCusumValues.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
            
        double[] negativeCusumArray = negativeCusumValues.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
        
        return new Result(positiveCusumArray, negativeCusumArray, anomalyIndex);
    }
}