package com.censoredsurvivors.data.statistics;

import smile.wavelet.HaarWavelet;
import smile.wavelet.Wavelet;

public class SocialMediaPostCountWavelets {
    private final Wavelet wavelet = new HaarWavelet();

    /**
     * Transforms the post counts using wavelet transform, then performs inverse transform.
     * Mostly used for testing.
     *
     * @param postCounts Array of post count data
     * @return Reconstructed post counts after inverse transform
     */
    public double[] identityTransform(double[] postCounts) {
        double[] transformed = transform(postCounts);
        wavelet.inverse(transformed);

        return transformed;
    }

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
        double[] coefficients = transform(postCounts);

        double[] isolated = isolateFrequencyLevel(coefficients, frequencyLevel);
        wavelet.inverse(isolated);

        return isolated;
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
        double[] coefficients = transform(postCounts);

        double[] isolated = isolateFrequencyLevels(coefficients, frequencyLevels);
        wavelet.inverse(isolated);

        return isolated;
    }

    /**
     * Isolates a specific frequency level in the wavelet coefficients.
     * All coefficients not in the specified level will be set to zero.
     *
     * @param coefficients The wavelet coefficients to modify
     * @param level Frequency level to isolate
     * @return Array of coefficients with the specified level isolated
     * @throws IllegalArgumentException if any level is invalid
     */
    public double[] isolateFrequencyLevel(double[] coeffs, int level) {
        int n = coeffs.length;
        int L = (int)(Math.log(n) / Math.log(2));
        if ((1 << L) != n) {
            throw new IllegalArgumentException("Coefficient array length must be a power of 2.");
        }
        if (level < 0 || level > L) {
            throw new IllegalArgumentException("Level must be between 0 and " + L + " but was " + level);
        }
        double[] isolated = new double[n];
        if (level == 0) {
            isolated[0] = coeffs[0];
        } else {
            int blockSize = 1 << (L - level);
            int start = blockSize;
            for (int i = 0; i < blockSize; i++) {
                isolated[start + i] = coeffs[start + i];
            }
        }
        return isolated;
    }

    /**
     * Isolates multiple frequency levels in the wavelet coefficients.
     * All coefficients not in the specified levels will be set to zero.
     *
     * @param coefficients The wavelet coefficients to modify
     * @param levels Array of frequency levels to preserve
     * @return Array of coefficients with the specified levels isolated
     * @throws IllegalArgumentException if any level is invalid
     */
    public double[] isolateFrequencyLevels(double[] coeffs, int[] levels) {
        int n = coeffs.length;
        int L = (int) (Math.log(n) / Math.log(2));
        if ((1 << L) != n) {
            throw new IllegalArgumentException("Coefficient array length must be a power of 2.");
        }
        double[] isolated = new double[n];
        for (int level : levels) {
            if (level < 0 || level > L) {
                throw new IllegalArgumentException("Level must be between 0 and " + L);
            }
            if (level == 0) {
                isolated[0] = coeffs[0];
            } else {
                int blockSize = 1 << (L - level);
                int start = blockSize;
                System.arraycopy(coeffs, start, isolated, start, blockSize);
            }
        }
        return isolated;
    }

    private int nextPowerOfTwo(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
}
