package com.github.epsilon.fabric;

import com.github.epsilon.Epsilon;
import com.github.epsilon.addon.EpsilonAddon;

/**
 * Built-in Fabric addon for Fabric-only features.
 */
public class FabricPlatformAddon extends EpsilonAddon {

    public FabricPlatformAddon() {
        super("epsilon_fabric");
    }

    @Override
    public void onSetup() {
        Epsilon.LOGGER.info("Fabric platform addon initialized.");
    }

}

