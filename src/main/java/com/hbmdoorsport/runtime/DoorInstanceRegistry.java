package com.hbmdoorsport.runtime;

import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Client weak registry so looped sounds survive BER culling. */
public final class DoorInstanceRegistry {
    private static final Set<RoundAirlockDoorBlockEntity> ROUND = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LegacyDoorBlockEntity> GENERIC = Collections.newSetFromMap(new WeakHashMap<>());

    private DoorInstanceRegistry() { }
    public static void trackClient(RoundAirlockDoorBlockEntity door) { ROUND.add(door); }
    public static void trackClient(LegacyDoorBlockEntity door) { GENERIC.add(door); }
    public static List<RoundAirlockDoorBlockEntity> snapshot() { return new ArrayList<>(ROUND); }
    public static List<LegacyDoorBlockEntity> snapshotGeneric() { return new ArrayList<>(GENERIC); }
}
