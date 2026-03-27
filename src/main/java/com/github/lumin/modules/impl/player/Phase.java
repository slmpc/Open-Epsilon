package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.player.FindItemResult;
import com.github.lumin.utils.player.InvUtils;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class Phase extends Module {

    public static final Phase INSTANCE = new Phase();

    private Phase() {
        super("Phase", Category.PLAYER);
    }

    private enum Mode {
        Pearl,
        Clip,
        Hybrid
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Hybrid);

    private final BoolSetting onlyOnGround = boolSetting("OnlyOnGround", false, this::usePearlMode);
    private final BoolSetting autoDisable = boolSetting("AutoDisable", false, this::usePearlMode);
    private final IntSetting pearlDelay = intSetting("PearlDelay", 20, 0, 60, 1, this::usePearlMode);
    private final IntSetting afterPearl = intSetting("AfterPearl", 0, 0, 60, 1, this::usePearlMode);
    private final IntSetting pitch = intSetting("Pitch", 80, 0, 90, 1, this::usePearlMode);
    private final BoolSetting swingHand = boolSetting("SwingHand", true, this::usePearlMode);

    private final IntSetting clipDelay = intSetting("ClipDelay", 4, 0, 20, 1, this::useClipMode);
    private final IntSetting clipDistance = intSetting("ClipDistance", 26, 5, 80, 1, this::useClipMode);

    private int clipTimer;
    private int afterPearlTimer;

    @Override
    protected void onEnable() {
        clipTimer = 0;
        afterPearlTimer = 0;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        if (clipTimer > 0) {
            clipTimer--;
        }
        if (afterPearlTimer > 0) {
            afterPearlTimer--;
        }

        if (mc.screen != null) {
            return;
        }

        boolean usePearl = usePearlMode();
        boolean useClip = useClipMode();

        boolean touchingWall = mc.player.horizontalCollision || (useClip && afterPearlTimer > 0);
        if (!touchingWall) {
            return;
        }

        if (usePearl && mc.player.horizontalCollision && canPearlPhase() && tryPearlPhase()) {
            clipTimer = pearlDelay.getValue();
            afterPearlTimer = afterPearl.getValue();
            if (autoDisable.getValue()) {
                toggle();
            }
            return;
        }

        if (!useClip || clipTimer > 0) {
            return;
        }

        if (tryVanillaClip()) {
            clipTimer = clipDelay.getValue();
        }
    }

    private boolean canPearlPhase() {
        return usePearlMode() && clipTimer <= 0 && (!onlyOnGround.getValue() || mc.player.onGround()) && mc.player.tickCount > 60 && !mc.player.isPassenger() && !mc.player.isUsingItem();
    }

    private boolean usePearlMode() {
        return mode.is(Mode.Pearl) || mode.is(Mode.Hybrid);
    }

    private boolean useClipMode() {
        return mode.is(Mode.Clip) || mode.is(Mode.Hybrid);
    }

    private boolean tryPearlPhase() {
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) {
            return false;
        }

        float prevPitch = mc.player.getXRot();

        InteractionHand hand = pearl.getHand();
        boolean swapped = InvUtils.swap(pearl.slot(), true);

        mc.player.setXRot(pitch.getValue().floatValue());

        mc.gameMode.useItem(mc.player, hand);

        if (swingHand.getValue()) {
            mc.player.swing(hand);
        } else {
            mc.getConnection().send(new ServerboundSwingPacket(hand));
        }

        mc.player.setXRot(prevPitch);

        if (swapped) {
            InvUtils.swapBack();
        }

        return true;
    }

    private boolean tryVanillaClip() {
        double distance = clipDistance.getValue() / 100.0;
        float yaw = mc.player.getYRot();
        double radians = Math.toRadians(yaw);
        double x = -Mth.sin((float) radians) * distance;
        double z = Mth.cos((float) radians) * distance;

        if (mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(x, 0.0, z))) {
            mc.player.setPos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
            return true;
        }

        double halfX = x * 0.5;
        double halfZ = z * 0.5;
        if (mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(halfX, 0.0, halfZ))) {
            mc.player.setPos(mc.player.getX() + halfX, mc.player.getY(), mc.player.getZ() + halfZ);
            return true;
        }

        return false;
    }

}
