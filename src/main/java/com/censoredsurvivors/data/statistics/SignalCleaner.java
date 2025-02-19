package com.censoredsurvivors.data.statistics;

public class SignalCleaner {

    public static final int WINDOW_SIZE = 5;

    private static final Wavelets wavelets = new Wavelets();

    public enum SignalCleaningType {
        NONE,
        INTERPOLATE_ZEROES,
        SIMPLE_MOVING_AVERAGE,
        SIMPLE_MOVING_AVERAGE_AND_INTERPOLATE_ZEROES,
        WAVELET_DENOISING,
        WAVELET_DENOISING_AND_INTERPOLATE_ZEROES,
    }

    public static double[] clean(double[] signal, SignalCleaningType signalCleaningType) {
        switch (signalCleaningType) {
            case NONE:
                return signal;
            case INTERPOLATE_ZEROES:
                return Interpolator.interpolateZeroes(signal);
            case SIMPLE_MOVING_AVERAGE:
                return MovingAverage.simpleMovingAverage(signal, WINDOW_SIZE);
            case SIMPLE_MOVING_AVERAGE_AND_INTERPOLATE_ZEROES:
                return MovingAverage.simpleMovingAverage(Interpolator.interpolateZeroes(signal), WINDOW_SIZE);
            case WAVELET_DENOISING:
                return wavelets.denoise(signal);
            case WAVELET_DENOISING_AND_INTERPOLATE_ZEROES:
                return wavelets.denoise(Interpolator.interpolateZeroes(signal));
            default:
                throw new IllegalArgumentException("Invalid signal cleaning type: " + signalCleaningType);
        }
    }
}
