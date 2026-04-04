package com.github.epsilon.managers;

import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.impl.render.ModuleList;
import com.github.epsilon.modules.Module;
import com.github.epsilon.modules.impl.ClientSetting;
import com.github.epsilon.modules.impl.combat.*;
import com.github.epsilon.modules.impl.player.*;
import com.github.epsilon.modules.impl.render.*;
import com.github.epsilon.modules.impl.world.AutoAccount;
import com.github.epsilon.modules.impl.world.AutoFarm;
import com.github.epsilon.modules.impl.world.FakePlayer;
import com.github.epsilon.modules.impl.world.Stealer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private List<Module> modules;

    private ModuleManager() {
        NeoForge.EVENT_BUS.register(this);
    }

    public void initModules() {
        modules = List.of(

                // Client
                ClientSetting.INSTANCE,

                // Combat
                AimAssist.INSTANCE,
                AntiBot.INSTANCE,
                AutoClicker.INSTANCE,
                AutoMend.INSTANCE,
                AutoTotem.INSTANCE,
                CrystalAura.INSTANCE,
                KillAura.INSTANCE,
                PacketMine.INSTANCE,
                Velocity.INSTANCE,

                // Player
                AutoSprint.INSTANCE,
                AutoTool.INSTANCE,
                BreakCooldown.INSTANCE,
                Disabler.INSTANCE,
                ElytraFly.INSTANCE,
                InvManager.INSTANCE,
                JumpCooldown.INSTANCE,
                MovementFix.INSTANCE,
                NoRotate.INSTANCE,
                NoSlow.INSTANCE,
                Phase.INSTANCE,
                PacketEat.INSTANCE,
                SafeWalk.INSTANCE,
                Scaffold.INSTANCE,
                Stuck.INSTANCE,
                UseCooldown.INSTANCE,
                VClip.INSTANCE,

                // Render
                ESP.INSTANCE,
                Fullbright.INSTANCE,
                ModuleList.INSTANCE,
                NameTags.INSTANCE,
                NoRender.INSTANCE,
                PopChams.INSTANCE,

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
        if (keyCode == ClientSetting.INSTANCE.guiKeybind.getValue()
                && action == InputConstants.PRESS
                && Minecraft.getInstance().screen == null
        ) {
            Minecraft.getInstance().setScreen(PanelScreen.INSTANCE);
        }

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

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {

        modules.forEach(module -> {
            if (module.isEnabled() && module instanceof HudModule hudModule) {
                RenderManager.INSTANCE.applyRenderHud(hudModule::render);
            }
        });

    }

}
