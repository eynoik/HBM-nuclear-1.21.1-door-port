package com.hbmdoorsport.block;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import com.hbmdoorsport.door.LegacyDoorType;
import com.hbmdoorsport.door.LegacyDoorType.LocalPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Shared multiblock shell for every OBJ-based DoorDecl port except the already-proven round airlock. */
public final class LegacyDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 63);
    public static final BooleanProperty OPEN_SHAPE = BooleanProperty.create("open_shape");

    private static boolean removingStructure;
    private final LegacyDoorType type;

    public LegacyDoorBlock(LegacyDoorType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, type.corePart())
                .setValue(OPEN_SHAPE, false));
    }

    public LegacyDoorType type() { return type; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new LegacyDoorBlock(type, p));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN_SHAPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction oldHbmFacing = context.getHorizontalDirection().getOpposite();
        return hasRoom(context.getLevel(), context.getClickedPos(), oldHbmFacing)
                ? defaultBlockState().setValue(FACING, oldHbmFacing).setValue(PART, type.corePart())
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(PART) != type.corePart()) return;

        Direction facing = state.getValue(FACING);
        for (int i = 0; i < type.partCount(); i++) {
            if (i == type.corePart()) continue;
            BlockPos p = worldPos(pos, facing, type.part(i));
            level.setBlock(p, defaultBlockState().setValue(FACING, facing).setValue(PART, i).setValue(OPEN_SHAPE, false), Block.UPDATE_ALL);
        }
    }

    private boolean hasRoom(LevelReader level, BlockPos core, Direction facing) {
        for (LocalPos local : type.parts()) {
            BlockPos p = worldPos(core, facing, local);
            if (p.equals(core)) continue;
            if (!level.getBlockState(p).canBeReplaced()) return false;
        }
        return true;
    }

    public static BlockPos worldPos(BlockPos core, Direction facing, LocalPos local) {
        return core.relative(facing.getCounterClockWise(), local.x())
                .relative(facing, local.z())
                .offset(0, local.y(), 0);
    }

    private boolean hasValidPart(BlockState state) {
        int part = state.getValue(PART);
        return part >= 0 && part < type.partCount();
    }

    public BlockPos corePos(BlockPos partPos, BlockState state) {
        if (!hasValidPart(state)) return partPos;
        LocalPos local = type.part(state.getValue(PART));
        Direction facing = state.getValue(FACING);
        return partPos.relative(facing.getCounterClockWise(), -local.x())
                .relative(facing, -local.z())
                .offset(0, -local.y(), 0);
    }

    @Nullable
    public LegacyDoorBlockEntity coreEntity(BlockGetter level, BlockPos partPos, BlockState state) {
        if (!hasValidPart(state)) return null;
        BlockEntity be = level.getBlockEntity(corePos(partPos, state));
        return be instanceof LegacyDoorBlockEntity door ? door : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            LegacyDoorBlockEntity door = coreEntity(level, pos, state);
            if (door != null) door.tryToggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            LegacyDoorBlockEntity door = coreEntity(level, pos, state);
            if (door != null) door.updateRedstone(anyPartPowered(level, door.getBlockPos(), state.getValue(FACING)));
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    public boolean anyPartPowered(Level level, BlockPos core, Direction facing) {
        for (LocalPos local : type.parts()) {
            if (level.hasNeighborSignal(worldPos(core, facing, local))) return true;
        }
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !removingStructure && hasValidPart(state)) {
            BlockPos core = corePos(pos, state);
            if (!player.isCreative()) popResource(level, core, new ItemStack(HbmDoorsPort.itemFor(type).get()));
            removeAllParts(level, core, state.getValue(FACING), pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !removingStructure && hasValidPart(state) && !state.is(newState.getBlock())) {
            removeAllParts(level, corePos(pos, state), state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void removeAllParts(Level level, BlockPos core, Direction facing, @Nullable BlockPos alreadyBreaking) {
        removingStructure = true;
        try {
            for (LocalPos local : type.parts()) {
                BlockPos p = worldPos(core, facing, local);
                if (alreadyBreaking != null && p.equals(alreadyBreaking)) continue;
                BlockState s = level.getBlockState(p);
                if (s.getBlock() == this) level.removeBlock(p, false);
            }
        } finally {
            removingStructure = false;
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collision(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collision(state);
    }

    private VoxelShape collision(BlockState state) {
        // PART is intentionally broad (0..63) so one shared property can serve every legacy door
        // type. State-cache mods such as MoreCulling enumerate those synthetic states too, so an
        // index outside this door type's actual part list must be harmless instead of reaching
        // LegacyDoorType.part(index) and crashing resource reload.
        if (!hasValidPart(state)) return Shapes.empty();
        LocalPos local = type.part(state.getValue(PART));
        VoxelShape shape = state.getValue(OPEN_SHAPE) ? type.openShape(local) : type.closedShape(local);
        return rotateShape(shape, state.getValue(FACING));
    }

    /** Exact metadata-specific local AABB rotation used by old BlockDoorGeneric. */
    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        if (shape.isEmpty() || facing == Direction.SOUTH) return shape;
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((minX,minY,minZ,maxX,maxY,maxZ) -> {
            double[] r = switch (facing) {
                case NORTH -> normalized(1-minX,minY,1-minZ,1-maxX,maxY,1-maxZ);
                case WEST -> normalized(1-minZ,minY,minX,1-maxZ,maxY,maxX);
                case EAST -> normalized(minZ,minY,1-minX,maxZ,maxY,1-maxX);
                default -> normalized(minX,minY,minZ,maxX,maxY,maxZ);
            };
            out[0] = Shapes.or(out[0], Shapes.box(r[0],r[1],r[2],r[3],r[4],r[5]));
        });
        return out[0];
    }

    private static double[] normalized(double x1,double y1,double z1,double x2,double y2,double z2) {
        return new double[]{Math.min(x1,x2),Math.min(y1,y2),Math.min(z1,z2),Math.max(x1,x2),Math.max(y1,y2),Math.max(z1,z2)};
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == type.corePart() ? new LegacyDoorBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (state.getValue(PART) != type.corePart()) return null;
        return createTickerHelper(beType, HbmDoorsPort.LEGACY_DOOR_BE.get(), LegacyDoorBlockEntity::tick);
    }
}
