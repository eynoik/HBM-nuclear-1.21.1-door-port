package com.hbmdoorsport.block;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 4x4 HBM Round Airlock multiblock.
 *
 * PART encoding:
 *   y = PART / 4       (0..3)
 *   lateral = PART % 4 - 2  (-2..+1)
 * Core is y=0, lateral=0 => PART=2.
 *
 * FACING intentionally stores the old HBM core direction (opposite the player),
 * which lets the renderer preserve the 1.12.2 orientation math directly.
 */
public final class RoundAirlockDoorBlock extends BaseEntityBlock {
    public static final MapCodec<RoundAirlockDoorBlock> CODEC = simpleCodec(RoundAirlockDoorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 15);
    public static final BooleanProperty OPEN_SHAPE = BooleanProperty.create("open_shape");
    public static final int CORE_PART = 2;

    private static boolean removingStructure;

    public RoundAirlockDoorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, CORE_PART)
                .setValue(OPEN_SHAPE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN_SHAPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction oldHbmFacing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, oldHbmFacing).setValue(PART, CORE_PART);
        return hasRoom(context.getLevel(), context.getClickedPos(), oldHbmFacing) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(PART) != CORE_PART) return;

        Direction facing = state.getValue(FACING);
        Direction lateralAxis = facing.getCounterClockWise();
        for (int y = 0; y < 4; y++) {
            for (int lateral = -2; lateral <= 1; lateral++) {
                int part = partOf(y, lateral);
                if (part == CORE_PART) continue;
                BlockPos p = pos.above(y).relative(lateralAxis, lateral);
                level.setBlock(p, defaultBlockState()
                        .setValue(FACING, facing)
                        .setValue(PART, part)
                        .setValue(OPEN_SHAPE, false), Block.UPDATE_ALL);
            }
        }
    }

    private static boolean hasRoom(LevelReader level, BlockPos core, Direction facing) {
        Direction lateralAxis = facing.getCounterClockWise();
        for (int y = 0; y < 4; y++) {
            for (int lateral = -2; lateral <= 1; lateral++) {
                BlockPos p = core.above(y).relative(lateralAxis, lateral);
                if (p.equals(core)) continue;
                if (!level.getBlockState(p).canBeReplaced()) return false;
            }
        }
        return true;
    }

    public static int partOf(int y, int lateral) {
        return y * 4 + (lateral + 2);
    }

    public static int yOf(int part) {
        return part / 4;
    }

    public static int lateralOf(int part) {
        return part % 4 - 2;
    }

    public static BlockPos corePos(BlockPos partPos, BlockState state) {
        int part = state.getValue(PART);
        int y = yOf(part);
        int lateral = lateralOf(part);
        Direction lateralAxis = state.getValue(FACING).getCounterClockWise();
        return partPos.below(y).relative(lateralAxis, -lateral);
    }

    @Nullable
    public static RoundAirlockDoorBlockEntity coreEntity(BlockGetter level, BlockPos partPos, BlockState state) {
        BlockEntity be = level.getBlockEntity(corePos(partPos, state));
        return be instanceof RoundAirlockDoorBlockEntity door ? door : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            RoundAirlockDoorBlockEntity door = coreEntity(level, pos, state);
            if (door != null) door.tryToggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            RoundAirlockDoorBlockEntity door = coreEntity(level, pos, state);
            if (door != null) door.updateRedstone(anyPartPowered(level, door.getBlockPos(), state.getValue(FACING)));
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    public static boolean anyPartPowered(Level level, BlockPos core, Direction facing) {
        Direction lateralAxis = facing.getCounterClockWise();
        for (int y = 0; y < 4; y++) {
            for (int lateral = -2; lateral <= 1; lateral++) {
                if (level.hasNeighborSignal(core.above(y).relative(lateralAxis, lateral))) return true;
            }
        }
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !removingStructure) {
            BlockPos core = corePos(pos, state);
            Direction facing = state.getValue(FACING);
            if (!player.isCreative()) {
                popResource(level, core, new ItemStack(HbmDoorsPort.ROUND_AIRLOCK_DOOR_ITEM.get()));
            }
            removeAllParts(level, core, facing, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !removingStructure && !state.is(newState.getBlock())) {
            removeAllParts(level, corePos(pos, state), state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeAllParts(Level level, BlockPos core, Direction facing, @Nullable BlockPos alreadyBreaking) {
        removingStructure = true;
        try {
            Direction lateralAxis = facing.getCounterClockWise();
            for (int y = 0; y < 4; y++) {
                for (int lateral = -2; lateral <= 1; lateral++) {
                    BlockPos p = core.above(y).relative(lateralAxis, lateral);
                    if (alreadyBreaking != null && p.equals(alreadyBreaking)) continue;
                    BlockState s = level.getBlockState(p);
                    if (s.is(HbmDoorsPort.ROUND_AIRLOCK_DOOR.get())) {
                        level.removeBlock(p, false);
                    }
                }
            }
        } finally {
            removingStructure = false;
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN_SHAPE) ? openFrameShape(state) : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN_SHAPE) ? openFrameShape(state) : Shapes.block();
    }

    private static VoxelShape openFrameShape(BlockState state) {
        int part = state.getValue(PART);
        int y = yOf(part);
        int lateral = lateralOf(part);

        // Exact local AABBs from DoorDecl.ROUND_AIRLOCK_DOOR#getBlockBound(open=true).
        // Interior parts become empty; edge/top/bottom parts keep the HBM frame collision.
        double minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
        boolean present = true;
        if (lateral == 1) {
            minX = 0.4; minY = 0; minZ = 0; maxX = 1; maxY = 1; maxZ = 1;
        } else if (lateral == -2) {
            minX = 0; minY = 0; minZ = 0; maxX = 0.6; maxY = 1; maxZ = 1;
        } else if (y == 3) {
            minX = 0; minY = 0.5; minZ = 0; maxX = 1; maxY = 1; maxZ = 1;
        } else if (y == 0) {
            minX = 0; minY = 0; minZ = 0; maxX = 1; maxY = 0.0625; maxZ = 1;
        } else {
            present = false;
        }
        if (!present) return Shapes.empty();

        double[] r = rotateAabb(minX, minY, minZ, maxX, maxY, maxZ, state.getValue(FACING));
        return Shapes.box(r[0], r[1], r[2], r[3], r[4], r[5]);
    }

    /** Reproduces BlockDoorGeneric's old metadata-specific AABB rotation. */
    private static double[] rotateAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Direction facing) {
        return switch (facing) {
            case NORTH -> normalized(1-minX, minY, 1-minZ, 1-maxX, maxY, 1-maxZ); // old meta 2
            case WEST  -> normalized(1-minZ, minY, minX, 1-maxZ, maxY, maxX);     // old meta 4
            case SOUTH -> normalized(minX, minY, minZ, maxX, maxY, maxZ);         // old meta 3
            case EAST  -> normalized(minZ, minY, 1-minX, maxZ, maxY, 1-maxX);     // old meta 5
            default -> normalized(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }

    private static double[] normalized(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new double[]{Math.min(x1,x2), Math.min(y1,y2), Math.min(z1,z2),
                Math.max(x1,x2), Math.max(y1,y2), Math.max(z1,z2)};
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CORE_PART ? new RoundAirlockDoorBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != CORE_PART) return null;
        return createTickerHelper(type, HbmDoorsPort.ROUND_AIRLOCK_DOOR_BE.get(), RoundAirlockDoorBlockEntity::tick);
    }
}
