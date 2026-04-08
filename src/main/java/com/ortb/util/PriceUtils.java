package com.ortb.util;

/**
 * Shared price calculation utilities used across auction services.
 */
public final class PriceUtils {

    private PriceUtils() {}

    /**
     * Rounds a price value to 2 decimal places.
     */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
