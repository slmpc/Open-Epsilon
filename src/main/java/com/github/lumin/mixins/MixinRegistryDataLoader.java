package com.github.lumin.mixins;

import com.github.lumin.Lumin;
import com.mojang.serialization.Decoder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TimelineTags;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.timeline.Timelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public abstract class MixinRegistryDataLoader {

    @Inject(method = "loadContentsFromNetwork", at = @At("TAIL"))
    private static <E> void lumin$restoreTimelineTags(
            Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> elements,
            ResourceProvider resourceProvider,
            RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> registry,
            Decoder<E> codec,
            Map<ResourceKey<?>, Exception> loadingErrors,
            CallbackInfo ci
    ) {
        if (!registry.key().equals(Registries.TIMELINE)) {
            return;
        }

        RegistryDataLoader.NetworkedRegistryData networkedRegistryData = elements.get(registry.key());
        if (networkedRegistryData == null || !networkedRegistryData.tags().isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        WritableRegistry<Timeline> timelineRegistry = (WritableRegistry<Timeline>) registry;
        HolderGetter<Timeline> holderGetter = timelineRegistry.createRegistrationLookup();

        LinkedHashSet<Holder<Timeline>> universal = new LinkedHashSet<>();
        holderGetter.get(Timelines.VILLAGER_SCHEDULE).ifPresent(universal::add);

        LinkedHashSet<Holder<Timeline>> inOverworld = new LinkedHashSet<>(universal);
        holderGetter.get(Timelines.DAY).ifPresent(inOverworld::add);
        holderGetter.get(Timelines.MOON).ifPresent(inOverworld::add);
        holderGetter.get(Timelines.EARLY_GAME).ifPresent(inOverworld::add);

        timelineRegistry.bindTag(TimelineTags.UNIVERSAL, List.copyOf(universal));
        timelineRegistry.bindTag(TimelineTags.IN_OVERWORLD, List.copyOf(inOverworld));
        timelineRegistry.bindTag(TimelineTags.IN_NETHER, List.copyOf(universal));
        timelineRegistry.bindTag(TimelineTags.IN_END, List.copyOf(universal));

        Lumin.LOGGER.warn("ViaForge omitted timeline tags during registry sync; applied local fallback timeline tags.");
    }

}
