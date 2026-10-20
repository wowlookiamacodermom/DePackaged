package com.example.madprojectnamedepackaged;

import android.content.Context;

import androidx.core.content.ContextCompat;

/**
 * Works out the "unhealth" score (0 = best, 10 = worst) and the numbers
 * shown on the Result screen.
 *
 * The "high" levels below follow the UK FSA front-of-pack traffic-light
 * limits per 100 g: sugar 22.5 g, saturated fat 5 g, salt 1.5 g (= 600 mg sodium).
 * Drinks use half the sugar limit. Daily reference values are common
 * WHO-style guidance for a 2000 kcal diet. Change the constants to tune it.
 */
public final class ScoreCalculator {

    public static final int GREEN = 0;
    public static final int AMBER = 1;
    public static final int RED = 2;

    // "High" levels per 100 g
    private static final double SUGAR_HIGH_G = 22.5;
    private static final double SUGAR_HIGH_DRINK_G = 11.25;
    private static final double SAT_FAT_HIGH_G = 5.0;
    private static final double SODIUM_HIGH_MG = 600.0;
    private static final double CALORIES_HIGH = 500.0;

    // Daily reference amounts
    public static final double DAILY_CALORIES = 2000;
    public static final double DAILY_SUGAR_G = 25;
    public static final double DAILY_SODIUM_MG = 2000;
    public static final double DAILY_SAT_FAT_G = 22;
    public static final double DAILY_FAT_G = 67;
    public static final double DAILY_PROTEIN_G = 50;

    private static final double SUGAR_PER_TEASPOON_G = 4.0;

    private ScoreCalculator() {
    }

    /** Score from 0 to 10, always worked out per 100 g so products compare fairly. */
    public static int score(Product p) {
        double sugarHigh = p.isBeverage() ? SUGAR_HIGH_DRINK_G : SUGAR_HIGH_G;
        double total = 4.0 * part(p.sugarG, sugarHigh)
                + 2.5 * part(p.satFatG, SAT_FAT_HIGH_G)
                + 2.5 * part(p.sodiumMg, SODIUM_HIGH_MG)
                + 1.0 * part(p.calories, CALORIES_HIGH);
        return (int) Math.round(total);
    }

    public static int ratingFor(int score) {
        if (score <= 3) {
            return GREEN;
        }
        if (score <= 5) {
            return AMBER;
        }
        return RED;
    }

    public static String ratingLabel(int rating) {
        switch (rating) {
            case GREEN:
                return "Better choice";
            case AMBER:
                return "Okay sometimes";
            default:
                return "Avoid";
        }
    }

    public static String verdict(int score) {
        switch (ratingFor(score)) {
            case GREEN:
                return "A better choice. Still read the label.";
            case AMBER:
                return "Okay now and then. Not an everyday food.";
            default:
                return "Ultra-processed. Think twice.";
        }
    }

    public static int colorFor(Context ctx, int rating) {
        switch (rating) {
            case GREEN:
                return ContextCompat.getColor(ctx, R.color.score_green);
            case AMBER:
                return ContextCompat.getColor(ctx, R.color.score_amber);
            default:
                return ContextCompat.getColor(ctx, R.color.score_red);
        }
    }

    public static int percent(double value, double dailyAmount) {
        if (dailyAmount <= 0) {
            return 0;
        }
        return (int) Math.round(value / dailyAmount * 100.0);
    }

    public static double teaspoons(double sugarGrams) {
        return sugarGrams / SUGAR_PER_TEASPOON_G;
    }

    // Per-100 g checks used for the red flags
    public static boolean highSodium(Product p) {
        return p.sodiumMg > SODIUM_HIGH_MG;
    }

    public static boolean highSatFat(Product p) {
        return p.satFatG > SAT_FAT_HIGH_G;
    }

    public static boolean highCalories(Product p) {
        return p.calories > 400;
    }

    private static double part(double value, double high) {
        return Math.max(0, Math.min(1, value / high));
    }
}
