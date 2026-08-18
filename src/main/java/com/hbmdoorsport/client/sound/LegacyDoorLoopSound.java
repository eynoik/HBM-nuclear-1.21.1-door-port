package com.hbmdoorsport.client.sound;

import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One original HBM loop sample attached to a moving generic door. */
public final class LegacyDoorLoopSound extends AbstractTickableSoundInstance {
    private final LegacyDoorBlockEntity door;

    public LegacyDoorLoopSound(LegacyDoorBlockEntity door, SoundEvent sound) {
        super(sound, SoundSource.BLOCKS, RandomSource.create());
        this.door = door;
        this.looping = true;
        this.delay = 0;
        this.volume = door.getSoundVolume();
        this.pitch = 1.0F;
        this.x = door.getBlockPos().getX() + 0.5;
        this.y = door.getBlockPos().getY() + 1.5;
        this.z = door.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (door.isRemoved()
                || mc.level == null
                || door.getLevel() != mc.level
                || mc.level.getBlockEntity(door.getBlockPos()) != door
                || !door.isMoving()) {
            stop();
        }
    }
}
