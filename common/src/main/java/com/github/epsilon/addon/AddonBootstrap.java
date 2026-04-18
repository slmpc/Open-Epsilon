package com.github.epsilon.addon;

import com.github.epsilon.Epsilon;

/**
 * Shared addon bootstrap utility used by multiple loaders.
 */
public final class AddonBootstrap {

    private AddonBootstrap() {
    }

    public static void setupAddons(EpsilonAddonSetupEvent addonEvent) {
        for (EpsilonAddon addon : addonEvent.getAddons()) {
            try {
                addon.onSetup();
                Epsilon.LOGGER.info("Loaded Epsilon addon: {}", addon.addonId);
            } catch (Throwable t) {
                Epsilon.LOGGER.error("Failed to setup Epsilon addon: {}", addon.addonId, t);
            }
        }
    }

}
