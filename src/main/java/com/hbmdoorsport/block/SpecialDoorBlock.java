package com.hbmdoorsport.block;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.blockentity.SpecialDoorBlockEntity;
import com.hbmdoorsport.door.SpecialDoorType;
import com.hbmdoorsport.door.SpecialDoorType.LocalPos;
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

/** Shared invisible multiblock shell for the five custom-rendered HBM doors. */
public final class SpecialDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part",0,700);
    public static final BooleanProperty OPEN_SHAPE = BooleanProperty.create("open_shape");

    private static boolean removingStructure;
    private final SpecialDoorType type;

    public SpecialDoorBlock(SpecialDoorType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,type.corePart()).setValue(OPEN_SHAPE,false));
    }

    public SpecialDoorType type() { return type; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new SpecialDoorBlock(type,p)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING,PART,OPEN_SHAPE); }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        return hasRoom(ctx.getLevel(),ctx.getClickedPos(),facing)
                ? defaultBlockState().setValue(FACING,facing).setValue(PART,type.corePart()) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (level.isClientSide || state.getValue(PART) != type.corePart()) return;
        Direction facing=state.getValue(FACING);
        for(int i=0;i<type.partCount();i++) {
            if(i==type.corePart()) continue;
            BlockPos p=worldPos(pos,facing,type.part(i));
            level.setBlock(p,defaultBlockState().setValue(FACING,facing).setValue(PART,i).setValue(OPEN_SHAPE,false),Block.UPDATE_ALL);
        }
    }

    private boolean hasRoom(LevelReader level,BlockPos core,Direction facing) {
        for(LocalPos lp:type.parts()) {
            BlockPos p=worldPos(core,facing,lp);
            if(!p.equals(core) && !level.getBlockState(p).canBeReplaced()) return false;
        }
        return true;
    }

    public static BlockPos worldPos(BlockPos core,Direction facing,LocalPos lp) {
        return core.relative(facing.getCounterClockWise(),lp.x()).relative(facing,lp.z()).offset(0,lp.y(),0);
    }

    public BlockPos corePos(BlockPos partPos,BlockState state) {
        LocalPos lp=type.part(state.getValue(PART)); Direction facing=state.getValue(FACING);
        return partPos.relative(facing.getCounterClockWise(),-lp.x()).relative(facing,-lp.z()).offset(0,-lp.y(),0);
    }

    @Nullable
    public SpecialDoorBlockEntity coreEntity(BlockGetter level,BlockPos pos,BlockState state) {
        BlockEntity be=level.getBlockEntity(corePos(pos,state));
        return be instanceof SpecialDoorBlockEntity d ? d : null;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide) { SpecialDoorBlockEntity d=coreEntity(level,pos,state); if(d!=null)d.tryToggle(); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moved) {
        if(!level.isClientSide) { SpecialDoorBlockEntity d=coreEntity(level,pos,state); if(d!=null)d.updateRedstone(anyPartPowered(level,d.getBlockPos(),state.getValue(FACING))); }
        super.neighborChanged(state,level,pos,neighbor,from,moved);
    }

    public boolean anyPartPowered(Level level,BlockPos core,Direction facing) {
        for(LocalPos lp:type.parts()) if(level.hasNeighborSignal(worldPos(core,facing,lp))) return true;
        return false;
    }

    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide && !removingStructure) {
            BlockPos core=corePos(pos,state);
            if(!player.isCreative()) popResource(level,core,new ItemStack(HbmDoorsPort.specialItemFor(type).get()));
            removeAll(level,core,state.getValue(FACING),pos);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }

    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState newState,boolean moved) {
        if(!level.isClientSide && !removingStructure && !state.is(newState.getBlock())) removeAll(level,corePos(pos,state),state.getValue(FACING),pos);
        super.onRemove(state,level,pos,newState,moved);
    }

    private void removeAll(Level level,BlockPos core,Direction facing,@Nullable BlockPos skip) {
        removingStructure=true;
        try { for(LocalPos lp:type.parts()) { BlockPos p=worldPos(core,facing,lp); if(skip!=null&&skip.equals(p))continue; if(level.getBlockState(p).getBlock()==this)level.removeBlock(p,false); } }
        finally { removingStructure=false; }
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx) { return collision(state); }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx) { return collision(state); }

    private VoxelShape collision(BlockState state) {
        LocalPos lp=type.part(state.getValue(PART));
        VoxelShape s=state.getValue(OPEN_SHAPE)?type.openShape(lp):type.closedShape(lp);
        return rotateShape(s,state.getValue(FACING));
    }

    private static VoxelShape rotateShape(VoxelShape shape,Direction facing) {
        if(shape.isEmpty()||facing==Direction.SOUTH)return shape;
        VoxelShape[] out={Shapes.empty()};
        shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->{
            double[] r=switch(facing){
                case NORTH->norm(1-x1,y1,1-z1,1-x2,y2,1-z2);
                case WEST->norm(1-z1,y1,x1,1-z2,y2,x2);
                case EAST->norm(z1,y1,1-x1,z2,y2,1-x2);
                default->norm(x1,y1,z1,x2,y2,z2);};
            out[0]=Shapes.or(out[0],Shapes.box(r[0],r[1],r[2],r[3],r[4],r[5]));
        }); return out[0];
    }
    private static double[] norm(double a,double b,double c,double d,double e,double f){return new double[]{Math.min(a,d),Math.min(b,e),Math.min(c,f),Math.max(a,d),Math.max(b,e),Math.max(c,f)};}

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)==type.corePart()?new SpecialDoorBlockEntity(pos,state):null;}
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> beType){
        if(state.getValue(PART)!=type.corePart())return null;
        return createTickerHelper(beType,HbmDoorsPort.SPECIAL_DOOR_BE.get(),SpecialDoorBlockEntity::tick);
    }
}
