package com.github.lumin.mixins;

import com.github.lumin.ducks.EntityEventPacketAccess;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundEntityEventPacket.class)
public interface MixinClientboundEntityEventPacket extends EntityEventPacketAccess {
    @Accessor("entityId")
    int lumin$getEntityId();

    @Accessor("eventId")
    byte lumin$getEventId();
}
