package com.github.epsilon.utils.math;

import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }

    public static int getRandom(int min, int max) {
        if (min == max) return min;
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static double getRandom(double min, double max) {
        if (min == max) return min;
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float getRandom(float min, float max) {
        if (min == max) return min;
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

}
