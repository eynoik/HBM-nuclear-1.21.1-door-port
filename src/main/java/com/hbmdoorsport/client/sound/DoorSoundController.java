package com.hbmdoorsport.client.sound;

import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;
import com.hbmdoorsport.runtime.DoorInstanceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class DoorSoundController {
    private static final Map<RoundAirlockDoorBlockEntity, RoundAirlockLoopSound> ROUND_ACTIVE = new WeakHashMap<>();
    private static final Map<LegacyDoorBlockEntity, List<LegacyDoorLoopSound>> GENERIC_ACTIVE = new WeakHashMap<>();

    private DoorSoundController() { }

    public static void tick() {
        for (RoundAirlockDoorBlockEntity door : DoorInstanceRegistry.snapshot()) ensureRound(door);
        for (LegacyDoorBlockEntity door : DoorInstanceRegistry.snapshotGeneric()) ensureGeneric(door);
    }

    private static void ensureRound(RoundAirlockDoorBlockEntity door) {
        if (!door.isMoving()) return;
        RoundAirlockLoopSound old = ROUND_ACTIVE.get(door);
        if (old != null && !old.isStopped()) return;
        RoundAirlockLoopSound sound = new RoundAirlockLoopSound(door);
        ROUND_ACTIVE.put(door, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void ensureGeneric(LegacyDoorBlockEntity door) {
        if (!door.isMoving()) return;
        List<LegacyDoorLoopSound> old = GENERIC_ACTIVE.get(door);
        if (old != null && old.stream().anyMatch(s -> !s.isStopped())) return;

        List<LegacyDoorLoopSound> now = new ArrayList<>(2);
        addLoop(door, door.getLoopSound(), now);
        addLoop(door, door.getSecondLoopSound(), now);
        if (!now.isEmpty()) GENERIC_ACTIVE.put(door, now);
    }

    private static void addLoop(LegacyDoorBlockEntity door, Supplier<SoundEvent> supplier, List<LegacyDoorLoopSound> out) {
        if (supplier == null) return;
        LegacyDoorLoopSound sound = new LegacyDoorLoopSound(door, supplier.get());
        out.add(sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
