package com.hbmdoorsport.blockentity;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.block.SpecialDoorBlock;
import com.hbmdoorsport.door.SpecialDoorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Shared state machine for the five special HBM door renderers. */
public final class SpecialDoorBlockEntity extends BlockEntity {
    public enum DoorState { CLOSED, OPENING, OPEN, CLOSING }

    private DoorState state=DoorState.CLOSED;
    private int movementTicks;
    private boolean redstonePower;
    private transient DoorState clientAnimState;
    private transient long clientAnimStartMillis;

    public SpecialDoorBlockEntity(BlockPos pos,BlockState state){super(HbmDoorsPort.SPECIAL_DOOR_BE.get(),pos,state);}
    public SpecialDoorType type(){return ((SpecialDoorBlock)getBlockState().getBlock()).type();}
    public DoorState state(){return state;}

    public static void tick(Level level,BlockPos pos,BlockState blockState,SpecialDoorBlockEntity door){
        SpecialDoorType type=door.type();
        if(door.state==DoorState.OPENING&&door.movementTicks<type.movementTicks())door.movementTicks++;
        else if(door.state==DoorState.CLOSING&&door.movementTicks>0)door.movementTicks--;

        if(!level.isClientSide){
            door.updateCollision();
            door.playTimedSounds();
            if(door.state==DoorState.OPENING&&door.movementTicks>=type.movementTicks()){door.movementTicks=type.movementTicks();door.state=DoorState.OPEN;door.playSpecialSound(true,false);door.sync();}
            else if(door.state==DoorState.CLOSING&&door.movementTicks<=0){door.movementTicks=0;door.state=DoorState.CLOSED;door.playSpecialSound(false,false);door.sync();}

            SpecialDoorBlock b=(SpecialDoorBlock)door.getBlockState().getBlock(); Direction f=door.getBlockState().getValue(SpecialDoorBlock.FACING);
            boolean powered=b.anyPartPowered(level,door.worldPosition,f);
            if(powered!=door.redstonePower){door.redstonePower=powered;door.setChanged();}
            if(powered&&door.state==DoorState.CLOSED)door.beginOpening(); else if(!powered&&door.state==DoorState.OPEN)door.beginClosing();
        }
    }

    public void tryToggle(){if(level==null||level.isClientSide)return;if(state==DoorState.CLOSED)beginOpening();else if(state==DoorState.OPEN)beginClosing();}
    public void updateRedstone(boolean p){if(level==null||level.isClientSide||p==redstonePower)return;redstonePower=p;if(p&&state==DoorState.CLOSED)beginOpening();else if(!p&&state==DoorState.OPEN)beginClosing();setChanged();}
    private void beginOpening(){if(state!=DoorState.CLOSED)return;state=DoorState.OPENING;playSpecialSound(true,true);sync();}
    private void beginClosing(){if(state!=DoorState.OPEN)return;state=DoorState.CLOSING;playSpecialSound(false,true);sync();}

    private void playSpecialSound(boolean opening,boolean start){
        if(level==null)return;
        HbmDoorsPort.playSpecialDoorSound(level,worldPosition,type(),opening,start);
    }

    private void playTimedSounds(){
        if(level==null||type()!=SpecialDoorType.VAULT_DOOR)return;
        if(state==DoorState.OPENING){
            if(movementTicks>=45&&movementTicks<=115&&(movementTicks-45)%10==0)
                level.playSound(null,worldPosition,HbmDoorsPort.VAULT_THUD.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
        } else if(state==DoorState.CLOSING){
            int elapsed=type().movementTicks()-movementTicks;
            if(elapsed>=0&&elapsed<=70&&elapsed%10==0)
                level.playSound(null,worldPosition,HbmDoorsPort.VAULT_THUD.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
            else if(elapsed==80)
                level.playSound(null,worldPosition,HbmDoorsPort.VAULT_SCRAPE.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
        }
    }

    private void updateCollision(){
        if(level==null)return; SpecialDoorType type=type(); float progress=movementTicks/(float)type.movementTicks();
        Direction facing=getBlockState().getValue(SpecialDoorBlock.FACING); SpecialDoorBlock block=(SpecialDoorBlock)getBlockState().getBlock();
        for(int i=0;i<type.partCount();i++){
            SpecialDoorType.LocalPos lp=type.part(i); float threshold=type.collisionThreshold(lp); boolean openShape;
            if(threshold>1.0F)openShape=false; else if(threshold<=0)openShape=movementTicks>0; else if(state==DoorState.CLOSING)openShape=progress>threshold; else openShape=progress>=threshold;
            BlockPos p=SpecialDoorBlock.worldPos(worldPosition,facing,lp); BlockState cur=level.getBlockState(p);
            if(cur.getBlock()==block&&cur.getValue(SpecialDoorBlock.OPEN_SHAPE)!=openShape)level.setBlock(p,cur.setValue(SpecialDoorBlock.OPEN_SHAPE,openShape),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Exact wall-clock animation progress, independent of server TPS like the 1.12 renderers. */
    public float renderProgress(){
        if(state==DoorState.CLOSED)return 0; if(state==DoorState.OPEN)return 1;
        long now=System.currentTimeMillis(); SpecialDoorType t=type();
        if(clientAnimState!=state||clientAnimStartMillis==0){long elapsed=state==DoorState.OPENING?(long)(movementTicks*50L):(long)((t.movementTicks()-movementTicks)*50L);clientAnimStartMillis=now-elapsed;clientAnimState=state;}
        long ms=Math.max(0,now-clientAnimStartMillis); float p=Math.max(0,Math.min(1,ms/(float)t.animationMillis())); return state==DoorState.CLOSING?1-p:p;
    }

    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider regs){super.saveAdditional(tag,regs);tag.putInt("state",state.ordinal());tag.putInt("movementTicks",movementTicks);tag.putBoolean("redstonePower",redstonePower);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider regs){super.loadAdditional(tag,regs);state=DoorState.values()[Math.max(0,Math.min(3,tag.getInt("state")))];movementTicks=Math.max(0,Math.min(type().movementTicks(),tag.getInt("movementTicks")));redstonePower=tag.getBoolean("redstonePower");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider regs){return saveCustomOnly(regs);}
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    private void sync(){if(level==null)return;setChanged();BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,Block.UPDATE_CLIENTS);}
}
