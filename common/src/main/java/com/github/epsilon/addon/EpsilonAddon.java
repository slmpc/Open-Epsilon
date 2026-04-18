package com.github.epsilon.addon;

import com.github.epsilon.assets.i18n.DefaultTranslateComponent;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Module;

/**
 * Base class for Epsilon addons.
 */
public abstract class EpsilonAddon {

    public final String addonId;

    protected EpsilonAddon(String addonId) {
        this.addonId = addonId;
    }

    /**
     * Called after this addon is registered.
     */
    public abstract void onSetup();

    protected void registerModule(Module module) {
        ModuleManager.INSTANCE.registerAddonModule(
                addonId,
                module,
                DefaultTranslateComponent.create(addonId + ".modules." + module.getName().toLowerCase())
        );
    }

}

