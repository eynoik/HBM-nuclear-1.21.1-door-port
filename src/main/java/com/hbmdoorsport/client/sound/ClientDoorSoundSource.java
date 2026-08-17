package com.hbmdoorsport.client.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public interface ClientDoorSoundSource {
    boolean isMoving();
    boolean isRemoved();
    BlockPos getBlockPos();
    float getSoundVolume();
    Supplier<SoundEvent> getLoopSound();
    Supplier<SoundEvent> getSecondLoopSound();
}
