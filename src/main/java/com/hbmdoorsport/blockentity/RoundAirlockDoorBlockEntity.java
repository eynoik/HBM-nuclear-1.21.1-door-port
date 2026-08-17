package com.hbmdoorsport.blockentity;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.block.RoundAirlockDoorBlock;
import com.hbmdoorsport.runtime.DoorInstanceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the state machine used by HBM TileEntityDoorGeneric for ROUND_AIRLOCK_DOOR.
 * Animation length is intentionally unchanged: 60 ticks = 3 seconds.
 */
public final class RoundAirlockDoorBlockEntity extends BlockEntity {
    public static final int OPEN_TICKS = 60;

    public enum DoorState { CLOSED, OPENING, OPEN, CLOSING }

    private DoorState state = DoorState.CLOSED;
    private int openTicks;
    private boolean redstonePower;

    private transient DoorState clientAnimState;
    private transient long clientAnimStartMillis;

    public RoundAirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmDoorsPort.ROUND_AIRLOCK_DOOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, RoundAirlockDoorBlockEntity door) {
        if (level.isClientSide) DoorInstanceRegistry.trackClient(door);
        if (door.state == DoorState.OPENING) {
            if (door.openTicks < OPEN_TICKS) door.openTicks++;
        } else if (door.state == DoorState.CLOSING) {
            if (door.openTicks > 0) door.openTicks--;
        }

        if (!level.isClientSide) {
            door.updateProgressiveCollision();

            if (door.state == DoorState.OPENING && door.openTicks >= OPEN_TICKS) {
                door.openTicks = OPEN_TICKS;
                door.state = DoorState.OPEN;
                door.finishMovement();
            } else if (door.state == DoorState.CLOSING && door.openTicks <= 0) {
                door.openTicks = 0;
                door.state = DoorState.CLOSED;
                door.finishMovement();
            }

            Direction facing = door.getBlockState().getValue(RoundAirlockDoorBlock.FACING);
            boolean powered = RoundAirlockDoorBlock.anyPartPowered(level, door.worldPosition, facing);
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
        if (level == null || level.isClientSide) return;
        if (redstonePower == powered) return;
        redstonePower = powered;
        setChanged();

        if (powered && state == DoorState.CLOSED) beginOpening();
        else if (!powered && state == DoorState.OPEN) beginClosing();
    }

    private void beginOpening() {
        if (level == null || state != DoorState.CLOSED) return;
        state = DoorState.OPENING;
        sync();
    }

    private void beginClosing() {
        if (level == null || state != DoorState.OPEN) return;
        state = DoorState.CLOSING;
        sync();
    }

    private void finishMovement() {
        if (level == null) return;
        level.playSound(null, worldPosition, HbmDoorsPort.ROUND_AIRLOCK_STOP.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        updateProgressiveCollision();
        sync();
    }

    private void updateProgressiveCollision() {
        if (level == null) return;
        float progress = openTicks / (float) OPEN_TICKS;
        Direction facing = getBlockState().getValue(RoundAirlockDoorBlock.FACING);
        Direction lateralAxis = facing.getCounterClockWise();

        for (int y = 0; y < 4; y++) {
            for (int lateral = -2; lateral <= 1; lateral++) {
                float threshold = switch (lateral) {
                    case -2 -> 1.0F;
                    case -1 -> 0.5F;
                    case 0 -> 0.0F;
                    case 1 -> 1.0F / 3.0F;
                    default -> 1.0F;
                };
                boolean openShape;
                if (threshold == 0.0F) {
                    openShape = openTicks > 0;
                } else if (state == DoorState.CLOSING) {
                    openShape = progress > threshold;
                } else {
                    openShape = progress >= threshold;
                }
                BlockPos p = worldPosition.above(y).relative(lateralAxis, lateral);
                BlockState current = level.getBlockState(p);
                if (current.is(HbmDoorsPort.ROUND_AIRLOCK_DOOR.get())
                        && current.getValue(RoundAirlockDoorBlock.OPEN_SHAPE) != openShape) {
                    level.setBlock(p, current.setValue(RoundAirlockDoorBlock.OPEN_SHAPE, openShape),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    public boolean isMoving() {
        return state == DoorState.OPENING || state == DoorState.CLOSING;
    }

    public DoorState getDoorState() {
        return state;
    }

    public float getLegacyRenderProgress() {
        if (state == DoorState.CLOSED) return 0.0F;
        if (state == DoorState.OPEN) return 1.0F;

        long now = System.currentTimeMillis();
        if (clientAnimState != state || clientAnimStartMillis == 0L) {
            long elapsedMillis = state == DoorState.OPENING
                    ? openTicks * 50L
                    : (OPEN_TICKS - openTicks) * 50L;
            clientAnimStartMillis = now - elapsedMillis;
            clientAnimState = state;
        }

        long ms = Math.max(0L, now - clientAnimStartMillis);
        long totalMs = OPEN_TICKS * 50L;
        long openingMillis = state == DoorState.CLOSING ? totalMs - ms : ms;
        float clamped = Math.max(0.0F, Math.min((float) totalMs, (float) openingMillis));
        return clamped / totalMs;
    }

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
        openTicks = Math.max(0, Math.min(OPEN_TICKS, tag.getInt("openTicks")));
        redstonePower = tag.getBoolean("redstonePower");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level == null) return;
        setChanged();
        BlockState s = getBlockState();
        level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
    }
}
