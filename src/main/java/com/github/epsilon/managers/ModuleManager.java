package com.github.epsilon.managers;

import com.github.epsilon.modules.Module;
import com.github.epsilon.modules.impl.client.ClickGui;
import com.github.epsilon.modules.impl.combat.*;
import com.github.epsilon.modules.impl.player.*;
import com.github.epsilon.modules.impl.render.*;
import com.github.epsilon.modules.impl.world.AutoAccount;
import com.github.epsilon.modules.impl.world.AutoFarm;
import com.github.epsilon.modules.impl.world.FakePlayer;
import com.github.epsilon.modules.impl.world.Stealer;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private List<Module> modules;

    private ModuleManager() {
    }

    public void initModules() {
        modules = List.of(

                // Client
                ClickGui.INSTANCE,

                // Combat
                AimAssist.INSTANCE,
                AntiBot.INSTANCE,
                AutoClicker.INSTANCE,
                CrystalAura.INSTANCE,
                KillAura.INSTANCE,
                PacketMine.INSTANCE,
                Velocity.INSTANCE,

                // Player
                BreakCooldown.INSTANCE,
                Disabler.INSTANCE,
                ElytraFly.INSTANCE,
                InvManager.INSTANCE,
                JumpCooldown.INSTANCE,
                MovementFix.INSTANCE,
                NoRotate.INSTANCE,
                NoSlow.INSTANCE,
                Phase.INSTANCE,
                SafeWalk.INSTANCE,
                Scaffold.INSTANCE,
                Sprint.INSTANCE,
                Stuck.INSTANCE,
                VClip.INSTANCE,

                // Render
                ESP.INSTANCE,
                Fullbright.INSTANCE,
                HUD.INSTANCE,
                ModuleList.INSTANCE,
                NoRender.INSTANCE,

                // World
                AutoFarm.INSTANCE,
                Stealer.INSTANCE,
                FakePlayer.INSTANCE,
                AutoAccount.INSTANCE

        );
    }

    public List<Module> getModules() {
        return modules;
    }

    public void onKeyEvent(int keyCode, int action) {
        for (final var module : modules) {
            if (module.getKeyBind() == keyCode) {
                if (module.getBindMode() == Module.BindMode.Hold) {
                    if (action == InputConstants.PRESS || action == InputConstants.REPEAT) {
                        module.setEnabled(true);
                    } else if (action == InputConstants.RELEASE) {
                        module.setEnabled(false);
                    }
                } else {
                    if (action == InputConstants.PRESS) {
                        module.toggle();
                    }
                }
            }
        }
    }

}
