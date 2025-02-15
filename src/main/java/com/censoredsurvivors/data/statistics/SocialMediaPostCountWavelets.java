package com.censoredsurvivors.data.statistics;

import com.censoredsurvivors.util.ProjectConfig;

import smile.wavelet.HaarWavelet;
import tech.tablesaw.api.Table;

public class SocialMediaPostCountWavelets {
    private final HaarWavelet wavelet = new HaarWavelet();

    public double[] transform(Table posts) {
        // Get original post counts
        double[] originalCounts = posts.stream()
            .mapToDouble(row -> row.getInt(ProjectConfig.POST_COUNT_COLUMN))
            .toArray();

        // Calculate next power of 2
        int targetLength = nextPowerOfTwo(originalCounts.length);

        System.out.println("Target length: " + targetLength);
        
        // Create padded array
        double[] postCounts = new double[targetLength];
        // Copy original values to the end of the new array, leaving zeros at the beginning
        System.arraycopy(originalCounts, 0, postCounts, targetLength - originalCounts.length, originalCounts.length);

        wavelet.transform(postCounts);

        return postCounts;
    }

    public double[] transformAndIsolateFrequency(Table posts, int frequencyLevel) {
        double[] postCounts = transform(posts);

        isolateFrequencyLevel(postCounts, frequencyLevel);
        wavelet.inverse(postCounts);

        return postCounts;
    }

    private void isolateFrequencyLevel(double[] coefficients, int level) {
        int n = coefficients.length;
        int numLevels = (int) (Math.log(n) / Math.log(2));
        
        if (level < 0 || level >= numLevels) {
            throw new IllegalArgumentException("Frequency level must be between 0 and " + (numLevels - 1) + " but was " + level);
        }

        // Calculate the start and end indices for the desired level
        int levelStart = n >>> (numLevels - level);
        int levelEnd = n >>> (numLevels - level - 1);

        // Zero out all coefficients except those in the desired level
        for (int i = 0; i < n; i++) {
            if (i < levelStart || i >= levelEnd) {
                coefficients[i] = 0.0;
            }
        }
    }

    private int nextPowerOfTwo(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
}
