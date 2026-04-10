package com.github.epsilon.addon;

import com.github.epsilon.assets.i18n.DefaultTranslateComponent;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Module;

/**
 * Base class for Epsilon addons.
 * <p>
 * Addon mods should listen for {@link EpsilonAddonSetupEvent} on {@code NeoForge.EVENT_BUS}
 * and call {@link EpsilonAddonSetupEvent#registerAddon(EpsilonAddon)} to register their addon.
 * The addon's {@link #onSetup()} will be called after all addons are registered.
 * </p>
 */
public abstract class EpsilonAddon {

    public final String addonId;

    public EpsilonAddon(String addonId) {
        this.addonId = addonId;
    }

    /**
     * Called after this addon is registered. Use this to register modules, etc.
     */
    public abstract void onSetup();

    /**
     * Registers a module under this addon.
     * The module's i18n keys will be prefixed with "{addonId}.modules.{moduleName}".
     *
     * @param module the module to register
     */
    protected void registerModule(Module module) {
        ModuleManager.INSTANCE.registerAddonModule(
                this,
                module,
                DefaultTranslateComponent.create(addonId + ".modules." + module.getName().toLowerCase())
        );
    }

}
