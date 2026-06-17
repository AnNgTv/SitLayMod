package me.duongdev.sitlay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SitLayMod implements ModInitializer {
    public static final Map<UUID, Entity> SEATS = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("sit")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player.hasVehicle()) {
                        player.sendMessage(new LiteralText("§cYou are already sitting or in a vehicle!"), false);
                        return 0;
                    }
                    sitPlayer(player, false);
                    return 1;
                }));

            dispatcher.register(CommandManager.literal("lay")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player.hasVehicle()) {
                        player.sendMessage(new LiteralText("§cYou are already in a vehicle!"), false);
                        return 0;
                    }
                    sitPlayer(player, true);
                    return 1;
                }));
        });

        // Sit on blocks
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || player.hasVehicle() || !player.getStackInHand(hand).isEmpty()) {
                return ActionResult.PASS;
            }

            BlockState state = world.getBlockState(hitResult.getBlockPos());
            if (state.getBlock() instanceof SlabBlock || state.getBlock() instanceof StairsBlock) {
                sitOnBlock((ServerPlayerEntity) player, hitResult.getBlockPos());
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Sit on other players
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            if (entity instanceof PlayerEntity targetPlayer) {
                if (player.isSneaking() && player.getStackInHand(hand).isEmpty()) {
                    if (!player.hasVehicle() && !targetPlayer.isPassenger()) {
                        player.startRiding(targetPlayer, true);
                        player.sendMessage(new LiteralText("§aYou are now sitting on §6" + targetPlayer.getName().getString()), false);
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            Entity seat = SEATS.remove(player.getUuid());
            if (seat != null) {
                seat.discard();
            }
        });
    }

    private void sitOnBlock(ServerPlayerEntity player, BlockPos pos) {
        World world = player.getEntityWorld();
        ArmorStandEntity seat = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        seat.setPos(pos.getX() + 0.5, pos.getY() + 0.5 - 1.2, pos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setInvulnerable(true);
        seat.setNoGravity(true);
        seat.setCustomName(new LiteralText("SitSeat"));
        seat.setCustomNameVisible(false);
        
        world.spawnEntity(seat);
        player.startRiding(seat, true);
        SEATS.put(player.getUuid(), seat);
    }

    private void sitPlayer(ServerPlayerEntity player, boolean lay) {
        World world = player.getEntityWorld();
        Vec3d pos = player.getPos();
        
        ArmorStandEntity seat = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        double heightOffset = lay ? 1.8 : 1.6;
        seat.setPos(pos.x, pos.y - heightOffset, pos.z);
        seat.setInvisible(true);
        seat.setInvulnerable(true);
        seat.setNoGravity(true);
        seat.setCustomName(new LiteralText(lay ? "LaySeat" : "SitSeat"));
        seat.setCustomNameVisible(false);
        
        world.spawnEntity(seat);
        player.startRiding(seat, true);
        
        if (lay) {
            player.sendMessage(new LiteralText("§aYou are now lying down."), false);
        } else {
            player.sendMessage(new LiteralText("§aYou are now sitting."), false);
        }
        
        SEATS.put(player.getUuid(), seat);
    }
}
