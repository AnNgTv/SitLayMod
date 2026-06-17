package me.luckyvn.sitlay.mixin;

import me.luckyvn.sitlay.SitLayMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void onStopRiding(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ServerPlayerEntity player) {
            if (SitLayMod.SEATS.containsKey(player.getUuid())) {
                Entity seat = SitLayMod.SEATS.remove(player.getUuid());
                if (seat != null) {
                    seat.discard();
                }
            }
        }
    }
}
