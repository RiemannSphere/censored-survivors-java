package com.censoredsurvivors.data.statistics;

import com.censoredsurvivors.util.ProjectConfig;

import smile.wavelet.HaarWavelet;
import tech.tablesaw.api.Table;

public class SocialMediaPostCountWavelets {
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

        HaarWavelet wavelet = new HaarWavelet();
        wavelet.transform(postCounts);

        return postCounts;
    }

    private int nextPowerOfTwo(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
}
