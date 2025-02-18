package com.censoredsurvivors.data.statistics;

import com.censoredsurvivors.util.ProjectConfig;

import smile.wavelet.HaarWavelet;
import smile.wavelet.Wavelet;
import tech.tablesaw.api.Table;

public class SocialMediaPostCountWavelets {
    private final Wavelet wavelet = new HaarWavelet();

    /**
     * Transforms the post counts using wavelet transform.
     *
     * @param postCounts Array of post count data
     * @return coefficients of the wavelet transform
     */
    public double[] transform(double[] postCounts) {
        int targetLength = nextPowerOfTwo(postCounts.length);
        
        // Create padded array
        double[] paddedPostCounts = new double[targetLength];
        // Copy original values to the end of the new array, leaving zeros at the beginning
        System.arraycopy(postCounts, 0, paddedPostCounts, targetLength - postCounts.length, postCounts.length);

        wavelet.transform(paddedPostCounts);

        return paddedPostCounts;
    }

    /**
     * Transforms the post counts using wavelet transform, isolates a specific frequency level,
     * and performs inverse transform.
     *
     * @param postCounts Array of post count data
     * @param frequencyLevel Frequency level to isolate
     * @return Reconstructed post counts after isolating the specified frequency level
     */
    public double[] reconstructByFrequency(double[] postCounts, int frequencyLevel) {
        double[] postCountsToReconstruct = transform(postCounts);

        isolateFrequencyLevel(postCountsToReconstruct, frequencyLevel);
        wavelet.inverse(postCountsToReconstruct);

        return postCountsToReconstruct;
    }

    /**
     * Transforms the post counts using wavelet transform, isolates specified frequency levels,
     * and performs inverse transform.
     *
     * @param postCounts Array of post count data
     * @param frequencyLevels Array of frequency levels to isolate
     * @return Reconstructed post counts after isolating the specified frequency levels
     */
    public double[] reconstructByFrequencies(double[] postCounts, int[] frequencyLevels) {
        double[] postCountsToReconstruct = transform(postCounts);

        isolateFrequencyLevels(postCountsToReconstruct, frequencyLevels);
        wavelet.inverse(postCountsToReconstruct);

        return postCountsToReconstruct;
    }

    /**
     * Isolates a specific frequency level in the wavelet coefficients.
     * All coefficients not in the specified level will be set to zero.
     *
     * @param coefficients The wavelet coefficients to modify
     * @param level Frequency level to isolate
     * @throws IllegalArgumentException if any level is invalid
     */
    private void isolateFrequencyLevel(double[] coefficients, int level) {
        int n = coefficients.length;
        int numLevels = (int) (Math.log(n) / Math.log(2));
        
        if (level < 0 || level >= numLevels) {
            throw new IllegalArgumentException("Frequency level must be between 0 and " + (numLevels - 1) + " but was " + level);
        }

        // Calculate the start and end indices for the desired level
        int levelStart = n / (int) Math.pow(2, numLevels - level);
        int levelEnd = n / (int) Math.pow(2, numLevels - level - 1);

        // Zero out all coefficients except those in the desired level
        for (int i = 0; i < n; i++) {
            if (i < levelStart || i >= levelEnd) {
                coefficients[i] = 0.0;
            }
        }
    }

    /**
     * Isolates multiple frequency levels in the wavelet coefficients.
     * All coefficients not in the specified levels will be set to zero.
     *
     * @param coefficients The wavelet coefficients to modify
     * @param levels Array of frequency levels to preserve
     * @throws IllegalArgumentException if any level is invalid
     */
    public void isolateFrequencyLevels(double[] coefficients, int[] levels) {
        int n = coefficients.length;
        int numLevels = (int) (Math.log(n) / Math.log(2));
        
        // Validate all levels first
        for (int level : levels) {
            if (level < 0 || level >= numLevels) {
                throw new IllegalArgumentException(
                    "Frequency level must be between 0 and " + (numLevels - 1) + " but was " + level
                );
            }
        }

        // Create a boolean array to mark which indices to keep
        boolean[] keepIndex = new boolean[n];
        
        // Mark indices for each level that should be preserved
        for (int level : levels) {
            int levelStart = n / (int) Math.pow(2, numLevels - level);
            int levelEnd = n / (int) Math.pow(2, numLevels - level - 1);
            
            for (int i = levelStart; i < levelEnd; i++) {
                keepIndex[i] = true;
            }
        }
        
        // Zero out all coefficients except those in the desired levels
        for (int i = 0; i < n; i++) {
            if (!keepIndex[i]) {
                coefficients[i] = 0.0;
            }
        }
    }

    private int nextPowerOfTwo(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
}
