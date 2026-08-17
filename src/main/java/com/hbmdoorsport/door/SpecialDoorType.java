package com.hbmdoorsport.door;

import com.hbmdoorsport.HbmDoorsPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Geometry/state data for the five legacy HBM doors which had their own renderers.
 * The renderers keep the original wall-clock durations and transform math.
 */
public enum SpecialDoorType {
    TRANSITION_SEAL("transition_seal", 480, 24040L, transitionParts()),
    BLAST_DOOR("blast_door", 100, 5000L, columnParts(7)),
    SLIDING_BLAST_DOOR("sliding_blast_door", 24, 1200L, rectangleParts(-3,3,0,3,0)),
    VAULT_DOOR("vault_door", 120, 6000L, vaultParts()),
    SILO_HATCH("silo_hatch", 100, 5000L, siloParts());

    public record LocalPos(int x, int y, int z) { }

    private final String id;
    private final int movementTicks;
    private final long animationMillis;
    private final List<LocalPos> parts;
    private final int corePart;

    SpecialDoorType(String id, int movementTicks, long animationMillis, List<LocalPos> parts) {
        this.id = id;
        this.movementTicks = movementTicks;
        this.animationMillis = animationMillis;
        this.parts = Collections.unmodifiableList(parts);
        int core = parts.indexOf(new LocalPos(0,0,0));
        if (core < 0) throw new IllegalStateException(id + " has no core position");
        this.corePart = core;
        if (parts.size() > 701) throw new IllegalStateException(id + " has too many parts: " + parts.size());
    }

    public String id() { return id; }
    public int movementTicks() { return movementTicks; }
    public long animationMillis() { return animationMillis; }
    public List<LocalPos> parts() { return parts; }
    public LocalPos part(int index) { return parts.get(index); }
    public int partCount() { return parts.size(); }
    public int corePart() { return corePart; }

    public ResourceLocation itemTexture() { return HbmDoorsPort.id("textures/item/" + id + ".png"); }

    /** Shape when the door is fully closed. */
    public VoxelShape closedShape(LocalPos p) {
        return Shapes.block();
    }

    /** Shape when the door is fully open. Frames remain where the original multiblocks did. */
    public VoxelShape openShape(LocalPos p) {
        return switch (this) {
            case TRANSITION_SEAL -> transitionFrame(p) ? Shapes.block() : Shapes.empty();
            case BLAST_DOOR -> (p.y() == 0 || p.y() == 6) ? Shapes.block() : Shapes.empty();
            case SLIDING_BLAST_DOOR -> (Math.abs(p.x()) == 3 || p.y() == 3) ? Shapes.block() : Shapes.empty();
            case VAULT_DOOR -> (Math.abs(p.x()) == 2 || p.y() == 0 || p.y() == 4) ? Shapes.block() : Shapes.empty();
            case SILO_HATCH -> siloCenter(p) ? Shapes.empty() : Shapes.block();
        };
    }

    /**
     * Progress at which the part stops being solid while opening. This mirrors the old
     * progressive dummy removal where the source used it, and keeps frame parts solid.
     */
    public float collisionThreshold(LocalPos p) {
        return switch (this) {
            case TRANSITION_SEAL -> transitionFrame(p) ? 2.0F : Math.max(0.0F, Math.min(0.92F, p.y() / 23.0F));
            case BLAST_DOOR -> {
                if (p.y() == 0 || p.y() == 6) yield 2.0F;
                yield switch (p.y()) { case 1 -> 0.0F; case 2 -> 0.2F; case 3 -> 0.4F; case 4 -> 0.6F; default -> 0.8F; };
            }
            case SLIDING_BLAST_DOOR -> {
                if (Math.abs(p.x()) == 3 || p.y() == 3) yield 2.0F;
                int ax = Math.abs(p.x());
                yield ax == 0 ? 0.50F : ax == 1 ? 0.67F : 0.83F;
            }
            case VAULT_DOOR -> (Math.abs(p.x()) == 2 || p.y() == 0 || p.y() == 4) ? 2.0F : 0.0F;
            case SILO_HATCH -> siloCenter(p) ? 0.70F : 2.0F;
        };
    }

    private static boolean transitionFrame(LocalPos p) {
        // Old opening range leaves the huge outer shell/frame in place.
        return p.y() >= 21 || p.x() <= -10 || p.x() >= 11;
    }

    private static boolean siloCenter(LocalPos p) {
        // Original TileEntitySiloHatch removes exactly the central 3x3 at timer 70.
        return Math.abs(p.x()) <= 1 && p.z() >= -4 && p.z() <= -2;
    }

    private static List<LocalPos> transitionParts() {
        List<LocalPos> out = new ArrayList<>();
        for (int y = 0; y <= 23; y++) for (int x = -13; x <= 12; x++) out.add(new LocalPos(x,y,0));
        return out;
    }

    private static List<LocalPos> columnParts(int height) {
        List<LocalPos> out = new ArrayList<>();
        for (int y=0;y<height;y++) out.add(new LocalPos(0,y,0));
        return out;
    }

    private static List<LocalPos> rectangleParts(int minX,int maxX,int minY,int maxY,int z) {
        List<LocalPos> out = new ArrayList<>();
        for (int y=minY;y<=maxY;y++) for (int x=minX;x<=maxX;x++) out.add(new LocalPos(x,y,z));
        return out;
    }

    private static List<LocalPos> vaultParts() {
        List<LocalPos> out = rectangleParts(-2,2,0,4,0);
        // Original vault also has the five lower teeth one block behind the main plane.
        for (int x=-2;x<=2;x++) out.add(new LocalPos(x,0,1));
        return out;
    }

    private static List<LocalPos> siloParts() {
        List<LocalPos> out = new ArrayList<>();
        // In the old block the 7x7 hatch is centered three blocks in front of the placement core.
        for (int ix=-3;ix<=3;ix++) {
            for (int iz=-3;iz<=3;iz++) {
                if ((Math.abs(ix)==3 && Math.abs(iz)>=2) || (Math.abs(iz)==3 && Math.abs(ix)>=2)) continue;
                out.add(new LocalPos(ix,0,iz-3));
            }
        }
        if (!out.contains(new LocalPos(0,0,0))) out.add(new LocalPos(0,0,0));
        return out;
    }
}
