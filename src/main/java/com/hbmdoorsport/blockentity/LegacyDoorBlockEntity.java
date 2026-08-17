package com.hbmdoorsport.blockentity;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.block.LegacyDoorBlock;
import com.hbmdoorsport.client.sound.ClientDoorSoundSource;
import com.hbmdoorsport.door.LegacyDoorType;
import com.hbmdoorsport.runtime.DoorInstanceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Shared DoorDecl state machine for all OBJ-based doors. */
public final class LegacyDoorBlockEntity extends BlockEntity implements ClientDoorSoundSource {
    public enum DoorState { CLOSED, OPENING, OPEN, CLOSING }

    private DoorState state = DoorState.CLOSED;
    private int openTicks;
    private boolean redstonePower;
    private transient DoorState clientAnimState;
    private transient long clientAnimStartMillis;

    public LegacyDoorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmDoorsPort.LEGACY_DOOR_BE.get(), pos, state);
    }

    public LegacyDoorType type() {
        return ((LegacyDoorBlock)getBlockState().getBlock()).type();
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, LegacyDoorBlockEntity door) {
        LegacyDoorType type = door.type();
        if (level.isClientSide) DoorInstanceRegistry.trackClient(door);

        if (door.state == DoorState.OPENING && door.openTicks < type.openTicks()) door.openTicks++;
        else if (door.state == DoorState.CLOSING && door.openTicks > 0) door.openTicks--;

        if (!level.isClientSide) {
            door.updateProgressiveCollision();

            if (door.state == DoorState.OPENING && door.openTicks >= type.openTicks()) {
                door.openTicks = type.openTicks();
                door.state = DoorState.OPEN;
                door.finishMovement(true);
            } else if (door.state == DoorState.CLOSING && door.openTicks <= 0) {
                door.openTicks = 0;
                door.state = DoorState.CLOSED;
                door.finishMovement(false);
            }

            LegacyDoorBlock block = (LegacyDoorBlock)door.getBlockState().getBlock();
            Direction facing = door.getBlockState().getValue(LegacyDoorBlock.FACING);
            boolean powered = block.anyPartPowered(level, door.worldPosition, facing);
            if (door.redstonePower != powered) {
                door.redstonePower = powered;
                door.setChanged();
            }
            if (powered && door.state == DoorState.CLOSED) door.beginOpening();
            else if (!powered && door.state == DoorState.OPEN) door.beginClosing();
        }
    }

    public void tryToggle() {
        if (level == null || level.isClientSide) return;
        if (state == DoorState.CLOSED) beginOpening();
        else if (state == DoorState.OPEN) beginClosing();
    }

    public void updateRedstone(boolean powered) {
        if (level == null || level.isClientSide || redstonePower == powered) return;
        redstonePower = powered;
        setChanged();
        if (powered && state == DoorState.CLOSED) beginOpening();
        else if (!powered && state == DoorState.OPEN) beginClosing();
    }

    private void beginOpening() {
        if (level == null || state != DoorState.CLOSED) return;
        state = DoorState.OPENING;
        playStart(true);
        sync();
    }

    private void beginClosing() {
        if (level == null || state != DoorState.OPEN) return;
        state = DoorState.CLOSING;
        playStart(false);
        sync();
    }

    private void playStart(boolean opening) {
        Supplier<SoundEvent> s = type().start(opening);
        if (s != null && level != null) level.playSound(null, worldPosition, s.get(), SoundSource.BLOCKS, type().soundVolume(), 1.0F);
    }

    private void finishMovement(boolean opening) {
        if (level == null) return;
        Supplier<SoundEvent> s = type().end(opening);
        if (s != null) level.playSound(null, worldPosition, s.get(), SoundSource.BLOCKS, type().soundVolume(), 1.0F);
        updateProgressiveCollision();
        sync();
    }

    private void updateProgressiveCollision() {
        if (level == null) return;
        LegacyDoorType type = type();
        float progress = openTicks / (float)type.openTicks();
        Direction facing = getBlockState().getValue(LegacyDoorBlock.FACING);
        LegacyDoorBlock block = (LegacyDoorBlock)getBlockState().getBlock();

        for (int i = 0; i < type.partCount(); i++) {
            LegacyDoorType.LocalPos local = type.part(i);
            float threshold = type.collisionThreshold(local);
            boolean openShape;
            if (threshold <= 0.0F) openShape = openTicks > 0;
            else if (state == DoorState.CLOSING) openShape = progress > threshold;
            else openShape = progress >= threshold;

            BlockPos p = LegacyDoorBlock.worldPos(worldPosition, facing, local);
            BlockState current = level.getBlockState(p);
            if (current.getBlock() == block && current.getValue(LegacyDoorBlock.OPEN_SHAPE) != openShape) {
                level.setBlock(p, current.setValue(LegacyDoorBlock.OPEN_SHAPE, openShape), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    @Override
    public boolean isMoving() { return state == DoorState.OPENING || state == DoorState.CLOSING; }
    public DoorState getDoorState() { return state; }

    public float getLegacyRenderProgress() {
        LegacyDoorType type = type();
        if (state == DoorState.CLOSED) return 0.0F;
        if (state == DoorState.OPEN) return 1.0F;

        long now = System.currentTimeMillis();
        if (clientAnimState != state || clientAnimStartMillis == 0L) {
            long elapsedMillis = state == DoorState.OPENING ? openTicks * 50L : (type.openTicks() - openTicks) * 50L;
            clientAnimStartMillis = now - elapsedMillis;
            clientAnimState = state;
        }
        long totalMs = type.openTicks() * 50L;
        long ms = Math.max(0L, now - clientAnimStartMillis);
        long openingMillis = state == DoorState.CLOSING ? totalMs - ms : ms;
        return Math.max(0.0F, Math.min(1.0F, openingMillis / (float)totalMs));
    }

    @Override
    public float getSoundVolume() { return type().soundVolume(); }
    @Override
    public Supplier<SoundEvent> getLoopSound() { return type().loop(); }
    @Override
    public Supplier<SoundEvent> getSecondLoopSound() { return type().secondLoop(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("state", state.ordinal());
        tag.putInt("openTicks", openTicks);
        tag.putBoolean("redstonePower", redstonePower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int stateId = Math.max(0, Math.min(DoorState.values().length - 1, tag.getInt("state")));
        state = DoorState.values()[stateId];
        int max = type().openTicks();
        openTicks = Math.max(0, Math.min(max, tag.getInt("openTicks")));
        redstonePower = tag.getBoolean("redstonePower");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void sync() {
        if (level == null) return;
        setChanged();
        BlockState s = getBlockState();
        level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
    }
}
