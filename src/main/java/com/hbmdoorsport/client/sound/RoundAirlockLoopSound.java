package com.hbmdoorsport.client.sound;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Original HBM garage_move.ogg, owned by the live moving block entity. */
public final class RoundAirlockLoopSound extends AbstractTickableSoundInstance {
    private final RoundAirlockDoorBlockEntity door;

    public RoundAirlockLoopSound(RoundAirlockDoorBlockEntity door) {
        super(HbmDoorsPort.ROUND_AIRLOCK_MOVE.get(), SoundSource.BLOCKS, RandomSource.create());
        this.door = door;
        this.looping = true;
        this.delay = 0;
        this.volume = 2.0F;
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
