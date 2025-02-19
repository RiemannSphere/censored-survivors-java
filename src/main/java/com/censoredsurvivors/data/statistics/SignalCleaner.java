package com.censoredsurvivors.data.statistics;

public class SignalCleaner {

    private static final Wavelets wavelets = new Wavelets();

    public enum SignalCleaningType {
        NONE,
        SIMPLE_MOVING_AVERAGE,
        WAVELET_DENOISING,
    }

    public static double[] clean(double[] signal, SignalCleaningType signalCleaningType) {
        switch (signalCleaningType) {
            case NONE:
                return signal;
            case SIMPLE_MOVING_AVERAGE:
                return MovingAverage.simpleMovingAverage(signal, 5);
            case WAVELET_DENOISING:
                return wavelets.denoise(signal);
            default:
                throw new IllegalArgumentException("Invalid signal cleaning type: " + signalCleaningType);
        }
    }
}
