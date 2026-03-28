package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.awt.*;

public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT);
    }

    /* General */
    private final DoubleSetting targetRange = doubleSetting("Target Range", 6.0, 0.0, 12.0, 0.5);
    private final DoubleSetting rotationSpeed = doubleSetting("Rotation Speed", 10.0, 1.0, 1.0, 0.5);
    private final BoolSetting eatingPause = boolSetting("Eating Pause", false);

    /* Calculation */
    private final BoolSetting noSuicide = boolSetting("No Suicide", true);
    private final DoubleSetting lethalMaxSelfDamage = doubleSetting("Lethal Max Self Dmg", 8.0, 0.0, 36.0, 0.25);
    private final BoolSetting motionPrediction = boolSetting("Motion Prediction", false);
    private final IntSetting predictTick = intSetting("Predict Tick", 6, 0, 10, 1, motionPrediction::getValue);

    /* Force Place */
    private final DoubleSetting forcePlaceHealth = doubleSetting("Force Place Health", 8.0, 0.0, 36.0, 0.5);
    private final IntSetting forcePlaceArmorRate = intSetting("Force Place Armor Rate", 3, 0, 25, 1);
    private final DoubleSetting forcePlaceMinDamage = doubleSetting("Force Place Min Dmg", 2.0, 0.0, 20.0, 0.25);
    private final DoubleSetting forcePlaceBalance = doubleSetting("Force Place Balance", -3.0, -10.0, 10.0, 0.25);

    /* Place */
    private final EnumSetting<SwingMode> placeSwing = enumSetting("Place Swing", SwingMode.None);
    private final EnumSetting<SwapMode> placeSwapMode = enumSetting("Place Swap Mode", SwapMode.None);
    private final DoubleSetting placeMinDmg = doubleSetting("Place Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting placeBalance = doubleSetting("Place Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 10);
    private final DoubleSetting placeRange = doubleSetting("Place Range", 4.0, 1.0, 6.0, 0.1);

    /* Break */
    private final EnumSetting<SwingMode> breakSwing = enumSetting("Break Swing", SwingMode.None);
    private final BoolSetting antiWeak = boolSetting("Anti Weak", false);
    private final EnumSetting<SwapMode> antiWeakSwapMode = enumSetting("Anti Weak Swap Mode", SwapMode.Silent, antiWeak::getValue);
    private final DoubleSetting breakMinDmg = doubleSetting("Break Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting breakBalance =  doubleSetting("Break Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting breakDelay = intSetting("Break Delay", 50, 0, 1000, 10);
    private final DoubleSetting breakRange = doubleSetting("Break Range", 4.0, 1.0, 6.0, 0.1);

    /* Render */
    private final ColorSetting filledColor = colorSetting("Filled Color", new Color(255, 150, 120, 100));
    private final ColorSetting outlineColor = colorSetting("Outline Color", new Color(255, 150, 120, 170));
    private final IntSetting movingLength = intSetting("Moving Length", 400, 0, 1000, 50);
    private final IntSetting fadeLength = intSetting("Fade Length", 200, 0, 1000, 50);

    private LivingEntity target;

    @Override
    protected void onDisable() {
        target = null;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        target = TargetManager.INSTANCE.acquirePrimary(
                TargetManager.TargetRequest.of(
                        targetRange.getValue(),
                        360.0f,
                        true,
                        false,
                        false,
                        false,
                        true,
                        1
                )
        );

        

    }

    private enum SwapMode {
        None,
        Swap,
        Silent,
    }

    private enum SwingMode {
        None,
        Client,
        Packet,
    }

}
