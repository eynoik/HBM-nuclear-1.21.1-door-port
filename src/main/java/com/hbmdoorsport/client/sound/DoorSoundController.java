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

/**
 * Owns the client-side HBM movement loops.
 *
 * A loop is valid only while the exact BlockEntity object is still installed at its world
 * position.  Checking only BlockEntity#isRemoved is not sufficient during multiblock teardown:
 * an old client BE can survive long enough for the controller to restart its loop after the
 * visible structure has already disappeared.
 */
public final class DoorSoundController {
    private static final Map<RoundAirlockDoorBlockEntity, RoundAirlockLoopSound> ROUND_ACTIVE = new WeakHashMap<>();
    private static final Map<LegacyDoorBlockEntity, List<LegacyDoorLoopSound>> GENERIC_ACTIVE = new WeakHashMap<>();

    private DoorSoundController() { }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            stopAll(mc);
            return;
        }

        // Kill stale loops before looking at the weak registry.  This prevents a dead moving BE
        // from being re-used as a sound source after its multiblock was destroyed.
        ROUND_ACTIVE.entrySet().removeIf(entry -> {
            RoundAirlockDoorBlockEntity door = entry.getKey();
            RoundAirlockLoopSound sound = entry.getValue();
            if (!isLive(mc, door) || !door.isMoving() || sound.isStopped()) {
                mc.getSoundManager().stop(sound);
                return true;
            }
            return false;
        });

        GENERIC_ACTIVE.entrySet().removeIf(entry -> {
            LegacyDoorBlockEntity door = entry.getKey();
            List<LegacyDoorLoopSound> sounds = entry.getValue();
            if (!isLive(mc, door) || !door.isMoving()) {
                for (LegacyDoorLoopSound sound : sounds) mc.getSoundManager().stop(sound);
                return true;
            }
            sounds.removeIf(LegacyDoorLoopSound::isStopped);
            return sounds.isEmpty();
        });

        for (RoundAirlockDoorBlockEntity door : DoorInstanceRegistry.snapshot()) {
            if (isLive(mc, door)) ensureRound(door);
        }
        for (LegacyDoorBlockEntity door : DoorInstanceRegistry.snapshotGeneric()) {
            if (isLive(mc, door)) ensureGeneric(door);
        }
    }

    private static boolean isLive(Minecraft mc, RoundAirlockDoorBlockEntity door) {
        return !door.isRemoved()
                && door.getLevel() == mc.level
                && mc.level != null
                && mc.level.getBlockEntity(door.getBlockPos()) == door;
    }

    private static boolean isLive(Minecraft mc, LegacyDoorBlockEntity door) {
        return !door.isRemoved()
                && door.getLevel() == mc.level
                && mc.level != null
                && mc.level.getBlockEntity(door.getBlockPos()) == door;
    }

    private static void stopAll(Minecraft mc) {
        for (RoundAirlockLoopSound sound : ROUND_ACTIVE.values()) mc.getSoundManager().stop(sound);
        for (List<LegacyDoorLoopSound> sounds : GENERIC_ACTIVE.values()) {
            for (LegacyDoorLoopSound sound : sounds) mc.getSoundManager().stop(sound);
        }
        ROUND_ACTIVE.clear();
        GENERIC_ACTIVE.clear();
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
