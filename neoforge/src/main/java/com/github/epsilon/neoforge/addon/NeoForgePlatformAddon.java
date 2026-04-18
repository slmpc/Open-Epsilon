package com.github.epsilon.neoforge.addon;

import com.github.epsilon.Epsilon;

/**
 * Built-in NeoForge addon for NeoForge-only features.
 */
public class NeoForgePlatformAddon extends com.github.epsilon.addon.EpsilonAddon {

    public NeoForgePlatformAddon() {
        super("epsilon_neoforge");
    }

    @Override
    public void onSetup() {
        Epsilon.LOGGER.info("NeoForge platform addon initialized.");
    }

}

