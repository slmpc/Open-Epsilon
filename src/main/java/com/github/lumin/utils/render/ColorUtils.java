package com.github.lumin.utils.render;

import net.minecraft.util.Mth;

import java.awt.*;

public class ColorUtils {

    private static int clamp255(int value) {
        return Mth.clamp(value, 0, 255);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Mth.clamp(opacity, 0.0f, 1.0f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static Color interpolateColor(Color color1, Color color2, float fraction) {
        fraction = Mth.clamp(fraction, 0.0f, 1.0f);

        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);

        return new Color(clamp255(red), clamp255(green), clamp255(blue), clamp255(alpha));
    }

}
