package com.censoredsurvivors.data.statistics;

import smile.wavelet.HaarWavelet;
import smile.wavelet.Wavelet;
import smile.wavelet.WaveletShrinkage;

public class Wavelets {
    private final Wavelet wavelet = new HaarWavelet();

    /**
     * Transforms the signal using wavelet transform, then performs inverse transform.
     * Mostly used for testing.
     *
     * @param signal Array of signal data
     * @return Reconstructed signal after inverse transform
     */
    public double[] identityTransform(double[] signal) {
        double[] transformed = transform(signal);
        wavelet.inverse(transformed);

        return transformed;
    }

    /**
     * Transforms the signal using wavelet transform.
     *
     * @param signal Array of signal data
     * @return coefficients of the wavelet transform
     */
    public double[] transform(double[] signal) {
        int targetLength = nextPowerOfTwo(signal.length);
        double[] paddedSignal = new double[targetLength];
        int paddingLength = targetLength - signal.length;
        System.arraycopy(signal, 0, paddedSignal, paddingLength, signal.length);

        wavelet.transform(paddedSignal);

        return paddedSignal;
    }

    /**
     * Denoises the signal using wavelet transform.
     *
     * @param signal Array of signal data
     * @return Denoised signal
     */
    public double[] denoise(double[] signal) {
        int targetLength = nextPowerOfTwo(signal.length);
        double[] paddedSignal = new double[targetLength];
        int paddingLength = targetLength - signal.length;
        System.arraycopy(signal, 0, paddedSignal, paddingLength, signal.length);

        WaveletShrinkage.denoise(paddedSignal, wavelet);

        return paddedSignal;
    }

    /**
     * Transforms the signal using wavelet transform, isolates a specific frequency level,
     * and performs inverse transform.
     *
     * @param signal Array of signal data
     * @param frequencyLevel Frequency level to isolate
     * @return Reconstructed signal after isolating the specified frequency level
     */
    public double[] reconstructByFrequency(double[] signal, int frequencyLevel) {
        double[] coefficients = transform(signal);

        double[] isolated = isolateFrequencyLevel(coefficients, frequencyLevel);
        wavelet.inverse(isolated);

        return isolated;
    }

    /**
     * Transforms the signal using wavelet transform, isolates specified frequency levels,
     * and performs inverse transform.
     *
     * @param signal Array of signal data
     * @param frequencyLevels Array of frequency levels to isolate
     * @return Reconstructed signal after isolating the specified frequency levels
     */
    public double[] reconstructByFrequencies(double[] signal, int[] frequencyLevels) {
        double[] coefficients = transform(signal);

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
