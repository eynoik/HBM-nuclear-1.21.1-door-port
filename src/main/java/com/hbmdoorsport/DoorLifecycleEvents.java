package com.hbmdoorsport;

import com.hbmdoorsport.blockentity.SpecialDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sound lifecycle glue which the old HBM AudioWrapper used to provide for us.
 * Vanilla positional sounds keep playing after their source block disappears, so explicitly
 * stop the HBM door samples when a door is broken. The stop packet is resource-scoped rather
 * than positional (vanilla has no positional stop packet), therefore it is only sent to players
 * close enough to have heard the destroyed door in the first place.
 */
@EventBusSubscriber(modid = HbmDoorsPort.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoorLifecycleEvents {
    private static final double STOP_RADIUS_SQR = 256.0D * 256.0D;

    private static final List<Supplier<SoundEvent>> DOOR_SOUNDS = List.of(
            HbmDoorsPort.ROUND_AIRLOCK_MOVE,
            HbmDoorsPort.ROUND_AIRLOCK_STOP,
            HbmDoorsPort.SLIDING_SEAL_MOVE,
            HbmDoorsPort.SLIDING_SEAL_STOP,
            HbmDoorsPort.WGH_MOVE,
            HbmDoorsPort.WGH_STOP,
            HbmDoorsPort.WGH_BIG_MOVE,
            HbmDoorsPort.WGH_BIG_STOP,
            HbmDoorsPort.ALARM6,
            HbmDoorsPort.QE_OPENING,
            HbmDoorsPort.QE_OPENED,
            HbmDoorsPort.QE_SHUT,
            HbmDoorsPort.HATCH_OPEN,
            HbmDoorsPort.LEVER,
            HbmDoorsPort.TRANSITION_SEAL_OPEN,
            HbmDoorsPort.REACTOR_START,
            HbmDoorsPort.REACTOR_STOP,
            HbmDoorsPort.VAULT_SCRAPE,
            HbmDoorsPort.VAULT_THUD,
            HbmDoorsPort.SILO_OPEN,
            HbmDoorsPort.SILO_CLOSE
    );

    private DoorLifecycleEvents() { }

    @SubscribeEvent
    public static void onDoorBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (!HbmDoorsPort.MODID.equals(id.getNamespace())) return;
        stopDoorSounds(level, event.getPos());
    }

    /**
     * HBM 1.12's Transition Seal only owns one transitionSealOpen sample; the generic door
     * audio path re-used the movement sample for the opposite direction. The port initially
     * only emitted it for OPENING, which made closing effectively silent/broken.
     */
    @SubscribeEvent
    public static void onTransitionSealClicked(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!HbmDoorsPort.id("transition_seal").equals(id)) return;
        if (!(level.getBlockEntity(pos) instanceof SpecialDoorBlockEntity door)) return;

        // Event fires before the block toggles. A fully-open render progress means this click
        // starts CLOSING. Opening already has its sound in HbmDoorsPort.playSpecialDoorSound().
        if (door.renderProgress() >= 0.99F) {
            stopSound(level, pos, HbmDoorsPort.TRANSITION_SEAL_OPEN);
            level.playSound(null, pos, HbmDoorsPort.TRANSITION_SEAL_OPEN.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
        }
    }

    private static void stopDoorSounds(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            if (!near(player, pos)) continue;
            for (Supplier<SoundEvent> sound : DOOR_SOUNDS) {
                ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound.get());
                player.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.BLOCKS));
            }
        }
    }

    private static void stopSound(ServerLevel level, BlockPos pos, Supplier<SoundEvent> sound) {
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound.get());
        ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(soundId, SoundSource.BLOCKS);
        for (ServerPlayer player : level.players()) {
            if (near(player, pos)) player.connection.send(packet);
        }
    }

    private static boolean near(ServerPlayer player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= STOP_RADIUS_SQR;
    }
}
