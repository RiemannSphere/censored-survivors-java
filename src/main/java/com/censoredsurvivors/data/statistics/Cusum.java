package com.censoredsurvivors.data.statistics;

import java.util.ArrayList;

public class Cusum {

    public record Result(
        double[] cusumValues, 
        int anomalyIndex
    ) {}

    /**
     * Computes both positive and negative CUSUM values in a single pass.
     * CUSUM (Cumulative Sum) is used to detect small shifts in a process mean.
     *
     * @param data          array of data points (x_1, x_2, ..., x_n)
     * @param sensitivity   reference value that determines how sensitive the algorithm is to changes.
     *                     Lower values make the algorithm more sensitive to small changes.
     * @param threshold     value at which to signal a significant change has been detected.
     *                     When CUSUM exceeds this value, it indicates an out-of-control condition.
     * @return             Result containing CUSUM values and the first anomaly index (-1 if no anomaly)
     */
    public static Result computeCusum(double[] data, double sensitivity, double threshold) {
        ArrayList<Double> cusumValues = new ArrayList<>();
        cusumValues.add(0.0);  // Initialize with 0
        int anomalyIndex = -1;
        
        for (int i = 0; i < data.length; i++) {
            // Calculate new CUSUM value using last element
            double newCusum = Math.max(0.0, cusumValues.getLast() + data[i] - sensitivity);
            cusumValues.add(newCusum);
            
            // Record first crossing of threshold
            if (newCusum > threshold && anomalyIndex == -1) {
                anomalyIndex = i;
            }
        }
        
        // Convert ArrayList to array for return value
        double[] cusumArray = cusumValues.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
        
        return new Result(cusumArray, anomalyIndex);
    }
}