package com.hbmdoorsport.door;

import com.hbmdoorsport.HbmDoorsPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Data-only transcription of HBM 1.12.2 DoorDecl for the OBJ-based doors. */
public enum LegacyDoorType {
    SLIDING_SEAL("sliding_seal_door", 15, dims(1,0,0,0,0,0), "sliding_seal_door.obj", "sliding_seal_door.png", 1.0F),
    SLIDING_GATE("sliding_gate_door", 28, dims(1,0,0,0,0,0), "sliding_seal_door.obj", "sliding_gate_door.png", 3.0F),
    SECURE_ACCESS("secure_access_door", 120, dims(4,0,0,0,2,2), "secure_access_door.obj", "secure_access_door.png", 2.0F),
    HATCH("hatch", 30, dims(0,0,0,0,0,0), "hatch.obj", "hatch.png", 2.0F),
    FIRE("fire_door", 160, dims(2,0,0,0,2,1), "fire_door.obj", "fire_door.png", 2.0F),
    QE_SLIDING("qe_sliding_door", 10, dims(1,0,0,0,1,0), "qe_sliding_door.obj", "qe_sliding_door.png", 2.0F),
    QE_CONTAINMENT("qe_containment_door", 160, dims(2,0,0,0,1,1), "qe_containment.obj", "qe_containment.png", 2.0F),
    WATER("water_door", 60, dims(2,0,0,0,1,1), "water_door.obj", "water_door.png", 2.0F),
    LARGE_VEHICLE("large_vehicle_door", 60, dims(5,0,0,0,3,3), "large_vehicle_door.obj", "large_vehicle_door.png", 2.0F);

    public record LocalPos(int x, int y, int z) {}

    private final String id;
    private final int openTicks;
    private final int[] dimensions;
    private final String modelFile;
    private final String textureFile;
    private final float soundVolume;
    private final List<LocalPos> parts;
    private final int corePart;

    private Supplier<SoundEvent> openStart;
    private Supplier<SoundEvent> closeStart;
    private Supplier<SoundEvent> moveLoop;
    private Supplier<SoundEvent> secondLoop;
    private Supplier<SoundEvent> openEnd;
    private Supplier<SoundEvent> closeEnd;

    LegacyDoorType(String id, int openTicks, int[] dimensions, String modelFile, String textureFile, float soundVolume) {
        this.id = id;
        this.openTicks = openTicks;
        this.dimensions = dimensions;
        this.modelFile = modelFile;
        this.textureFile = textureFile;
        this.soundVolume = soundVolume;

        List<LocalPos> p = new ArrayList<>();
        int core = -1;
        for (int y = -dimensions[1]; y <= dimensions[0]; y++) {
            for (int x = -dimensions[4]; x <= dimensions[5]; x++) {
                for (int z = -dimensions[2]; z <= dimensions[3]; z++) {
                    if (x == 0 && y == 0 && z == 0) core = p.size();
                    p.add(new LocalPos(x, y, z));
                }
            }
        }
        if (p.size() > 64) throw new IllegalStateException(id + " has too many multiblock parts: " + p.size());
        this.parts = Collections.unmodifiableList(p);
        this.corePart = core;
    }

    private static int[] dims(int u, int d, int n, int s, int w, int e) {
        return new int[]{u,d,n,s,w,e};
    }

    public String id() { return id; }
    public int openTicks() { return openTicks; }
    public int[] dimensions() { return dimensions.clone(); }
    public float soundVolume() { return soundVolume; }
    public int partCount() { return parts.size(); }
    public int corePart() { return corePart; }
    public LocalPos part(int index) { return parts.get(index); }
    public List<LocalPos> parts() { return parts; }
    public ResourceLocation model() { return HbmDoorsPort.id("models/doors/" + modelFile); }
    public ResourceLocation texture() { return HbmDoorsPort.id("textures/models/doors/" + textureFile); }
    public ResourceLocation decalTexture() { return HbmDoorsPort.id("textures/models/doors/qe_containment_decal.png"); }

    public LegacyDoorType sounds(Supplier<SoundEvent> openStart, Supplier<SoundEvent> closeStart,
                                 Supplier<SoundEvent> loop, Supplier<SoundEvent> secondLoop,
                                 Supplier<SoundEvent> openEnd, Supplier<SoundEvent> closeEnd) {
        this.openStart = openStart;
        this.closeStart = closeStart;
        this.moveLoop = loop;
        this.secondLoop = secondLoop;
        this.openEnd = openEnd;
        this.closeEnd = closeEnd;
        return this;
    }

    public Supplier<SoundEvent> start(boolean opening) { return opening ? openStart : (closeStart != null ? closeStart : openStart); }
    public Supplier<SoundEvent> loop() { return moveLoop; }
    public Supplier<SoundEvent> secondLoop() { return secondLoop; }
    public Supplier<SoundEvent> end(boolean opening) { return opening ? openEnd : (closeEnd != null ? closeEnd : openEnd); }

    public static float smoothstep(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    public float segment(float progress, int startTick, int endTick) {
        float ticks = progress * openTicks;
        return Math.max(0.0F, Math.min(1.0F, (ticks - startTick) / (float)(endTick - startTick)));
    }

    /** Closed local collision copied from DoorDecl#getBlockBound(open=false). */
    public VoxelShape closedShape(LocalPos p) {
        return switch (this) {
            case SLIDING_SEAL, SLIDING_GATE -> Shapes.box(0,0,0.75,1,1,1);
            case QE_SLIDING -> Shapes.box(0,0,0.875,1,1,1);
            case QE_CONTAINMENT -> Shapes.box(0,0,0.5,1,1,1);
            case WATER -> Shapes.box(0,0,0.75,1,1,1);
            default -> Shapes.block();
        };
    }

    /** Open-frame collision copied from each DoorDecl implementation. */
    public VoxelShape openShape(LocalPos p) {
        int lateral = p.x();
        int y = p.y();
        return switch (this) {
            case SLIDING_SEAL, SLIDING_GATE -> y == 0 ? Shapes.box(0,0,0.75,1,0.125,1) : Shapes.empty();
            case SECURE_ACCESS -> {
                if (y == 1) yield Shapes.box(0,0,0,1,0.0625,1);
                if (y == 4) yield Shapes.box(0,0.5,0.15,1,1,0.85);
                yield Shapes.empty();
            }
            case HATCH -> Shapes.box(0,0,0,1,1,0.0625);
            case FIRE -> {
                if (lateral == 1) yield Shapes.box(0.5,0,0,1,1,1);
                if (lateral == -2) yield Shapes.box(0,0,0,0.5,1,1);
                if (y > 1) yield Shapes.box(0,0.75,0,1,1,1);
                if (y == 0) yield Shapes.box(0,0,0,1,0.1,1);
                yield Shapes.empty();
            }
            case QE_SLIDING -> lateral == 0
                    ? Shapes.box(0.875,0,0.875,1,1,1)
                    : Shapes.box(0,0,0.875,0.125,1,1);
            case QE_CONTAINMENT -> {
                if (y > 1) yield Shapes.box(0,0.5,0.5,1,1,1);
                if (y == 0) yield Shapes.box(0,0,0.5,1,0.1,1);
                yield Shapes.empty();
            }
            case WATER -> {
                if (y > 1) yield Shapes.box(0,0.85,0.75,1,1,1);
                if (y == 0) yield Shapes.box(0,0,0.75,1,0.15,1);
                yield Shapes.empty();
            }
            case LARGE_VEHICLE -> {
                if (lateral == 3) yield Shapes.box(0.4,0,0,1,1,1);
                if (lateral == -3) yield Shapes.box(0,0,0,0.6,1,1);
                yield Shapes.empty();
            }
        };
    }

    /**
     * Collision-release timing. For simple doors this exactly follows the old opening range;
     * for the taller range-based doors it preserves the progressive feel rather than dropping
     * the entire wall on the first tick.
     */
    public float collisionThreshold(LocalPos p) {
        int x = p.x(), y = p.y();
        return switch (this) {
            case SLIDING_SEAL, SLIDING_GATE, HATCH, QE_SLIDING -> 0.0F;
            case SECURE_ACCESS -> y <= 0 ? 0.15F : Math.max(0.0F, Math.min(1.0F, (y - 1) / 3.0F));
            case FIRE -> Math.max(0.0F, Math.min(1.0F, y / 2.0F));
            case QE_CONTAINMENT -> Math.max(0.0F, Math.min(1.0F, y / 2.0F));
            case WATER -> 35.0F / 60.0F;
            case LARGE_VEHICLE -> {
                if (x < 0) yield Math.abs(x + 1) / 3.0F;
                yield x / 3.0F;
            }
        };
    }
}
