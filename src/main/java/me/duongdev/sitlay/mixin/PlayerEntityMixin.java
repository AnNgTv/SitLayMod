package me.duongdev.sitlay.mixin;

import me.duongdev.sitlay.SitLayMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void onGetPose(CallbackInfoReturnable<EntityPose> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Entity vehicle = player.getVehicle();
        if (vehicle != null && SitLayMod.SEATS.containsKey(player.getUuid())) {
            Entity seat = SitLayMod.SEATS.get(player.getUuid());
            if (seat != null && "LaySeat".equals(seat.getCustomName().getString())) {
                cir.setReturnValue(EntityPose.SLEEPING);
            }
        }
    }
}
