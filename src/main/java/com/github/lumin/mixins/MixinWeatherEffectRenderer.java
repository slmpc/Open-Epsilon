package com.github.lumin.mixins;

import com.github.lumin.modules.impl.world.CustomWeather;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {

    @ModifyReturnValue(method = "getPrecipitationAt", at = @At("RETURN"))
    private Biome.Precipitation onGetPrecipitationAt(Biome.Precipitation original) {
        if (original == Biome.Precipitation.NONE || !CustomWeather.INSTANCE.isSnowMode()) {
            return original;
        }

        return Biome.Precipitation.SNOW;
    }

}
