package com.censoredsurvivors.data.statistics;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

public class Interpolator {

    public static double[] interpolateZeroes(double[] signal) {
        int n = signal.length;
        // Count non-zero entries to size the arrays
        int count = 0;
        for (double v : signal) {
            if (v != 0) count++;
        }

        // If no non-zero values or just one, return the original signal
        if (count <= 1) {
            return signal.clone();
        }
        
        double[] x = new double[count];
        double[] y = new double[count];
        
        // Build x (indices) and y (non-zero values) arrays
        int index = 0;
        for (int i = 0; i < n; i++) {
            if (signal[i] != 0) {
                x[index] = i;
                y[index] = signal[i];
                index++;
            }
        }
        
        // Create the linear interpolation function
        LinearInterpolator interpolator = new LinearInterpolator();
        UnivariateFunction function = interpolator.interpolate(x, y);
        
        // Fill in the interpolated values
        double[] interpolatedSignal = new double[n];
        for (int i = 0; i < n; i++) {
            if (signal[i] == 0) {
                if (i < x[0]) {
                    // Before first non-zero value, use the first non-zero value
                    interpolatedSignal[i] = y[0];
                } else if (i > x[count - 1]) {
                    // After last non-zero value, use the last non-zero value
                    interpolatedSignal[i] = y[count - 1];
                } else {
                    // In between non-zero values, use interpolation
                    interpolatedSignal[i] = function.value(i);
                }
            } else {
                interpolatedSignal[i] = signal[i];
            }
        }
        
        return interpolatedSignal;
    }
    
    // For testing
    public static void main(String[] args) {
        double[] signal = {10, 31, 0, 20, 12, 0, 0, 0, 34, 14, 0, 23};
        double[] result = interpolateZeroes(signal);
        for (double v : result) {
            System.out.print(v + " ");
        }
    }
}

