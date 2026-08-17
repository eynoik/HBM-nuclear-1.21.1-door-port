package com.hbmdoorsport.client;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.block.SpecialDoorBlock;
import com.hbmdoorsport.blockentity.SpecialDoorBlockEntity;
import com.hbmdoorsport.client.model.LegacyColladaModel;
import com.hbmdoorsport.client.model.LegacyObjModel;
import com.hbmdoorsport.door.SpecialDoorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/** Ports the five bespoke HBM 1.12 door renderers without changing their timing math. */
public final class SpecialDoorRenderer implements BlockEntityRenderer<SpecialDoorBlockEntity> {
    private final Map<String,LegacyObjModel> obj=new HashMap<>();
    private LegacyColladaModel transition,sliding,silo;

    public SpecialDoorRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(SpecialDoorBlockEntity door,float partialTick,PoseStack ps,MultiBufferSource buffers,int light,int overlay) {
        SpecialDoorType type=door.type(); Direction facing=door.getBlockState().getValue(SpecialDoorBlock.FACING); float p=door.renderProgress();
        switch(type) {
            case TRANSITION_SEAL -> renderTransition(facing,p,ps,buffers,light,overlay);
            case BLAST_DOOR -> renderBlast(facing,p,ps,buffers,light,overlay);
            case SLIDING_BLAST_DOOR -> renderSliding(facing,p,ps,buffers,light,overlay);
            case VAULT_DOOR -> renderVault(facing,p,ps,buffers,light,overlay);
            case SILO_HATCH -> renderSilo(facing,p,ps,buffers,light,overlay);
        }
    }

    private void renderTransition(Direction facing,float p,PoseStack ps,MultiBufferSource b,int l,int o) {
        if(transition==null)transition=LegacyColladaModel.load(id("models/doors/seal.dae"),id("models/doors/seal.dae"),true);
        ps.pushPose(); ps.translate(.5,0,.5); ps.mulPose(Axis.YP.rotationDegrees(270F-facing.toYRot())); ps.translate(0,0,.5);
        VertexConsumer out=b.getBuffer(RenderType.entityCutoutNoCull(id("textures/models/doors/transition_seal.png")));
        transition.render(ps,out,l,o,p); ps.popPose();
    }

    private void renderSliding(Direction facing,float p,PoseStack ps,MultiBufferSource b,int l,int o) {
        if(sliding==null)sliding=LegacyColladaModel.load(id("models/anim/door0.dae"),id("models/anim/door0.dae"),false);
        ps.pushPose(); ps.translate(.5,0,.5); ps.mulPose(Axis.YP.rotationDegrees(180F-facing.toYRot()));
        VertexConsumer out=b.getBuffer(RenderType.entityTranslucent(id("textures/models/doors/slidingblast/sliding_blast_door.png")));
        sliding.render(ps,out,l,o,p); ps.popPose();
    }

    private void renderSilo(Direction facing,float p,PoseStack ps,MultiBufferSource b,int l,int o) {
        if(silo==null)silo=LegacyColladaModel.load(id("models/anim/hatch.dae"),id("models/anim/hatch.dae"),false);
        ps.pushPose(); ps.translate(.5,.595,.5);
        float rot=switch(facing){case NORTH->270F;case SOUTH->90F;case WEST->0F;case EAST->180F;default->0F;};
        ps.mulPose(Axis.YP.rotationDegrees(rot)); ps.translate(3,0,0);
        VertexConsumer out=b.getBuffer(RenderType.entityCutoutNoCull(id("textures/models/doors/hatchtexture.png")));
        silo.render(ps,out,l,o,p); ps.popPose();
    }

    private void renderBlast(Direction facing,float p,PoseStack ps,MultiBufferSource b,int l,int o) {
        ps.pushPose(); ps.translate(.5,0,.5); ps.mulPose(Axis.YP.rotationDegrees(180F));
        if(facing.getAxis()==Direction.Axis.Z)ps.mulPose(Axis.YP.rotationDegrees(90F));
        renderObj("blast_door_base.obj",id("textures/models/doors/blast/blast_door_base.png"),ps,b,l,o);
        ps.translate(0,3,0); renderObj("blast_door_block.obj",id("textures/models/doors/blast/blast_door_block.png"),ps,b,l,o);
        double timer=5.0*(1.0-p); ps.translate(0,-timer,0); ps.translate(0,2,0);
        renderObj("blast_door_tooth.obj",id("textures/models/doors/blast/blast_door_tooth.png"),ps,b,l,o);
        if(timer>1)renderObj("blast_door_slider.obj",id("textures/models/doors/blast/blast_door_slider.png"),ps,b,l,o);
        if(timer>2){ps.translate(0,1,0);renderObj("blast_door_slider.obj",id("textures/models/doors/blast/blast_door_slider.png"),ps,b,l,o);}
        if(timer>3){ps.translate(0,1,0);renderObj("blast_door_slider.obj",id("textures/models/doors/blast/blast_door_slider.png"),ps,b,l,o);}
        if(timer>4){ps.translate(0,1,0);renderObj("blast_door_slider.obj",id("textures/models/doors/blast/blast_door_slider.png"),ps,b,l,o);}
        ps.popPose();
    }

    private void renderVault(Direction facing,float p,PoseStack ps,MultiBufferSource b,int l,int o) {
        ps.pushPose(); ps.translate(.5,0,.5); ps.mulPose(Axis.YP.rotationDegrees(270F-facing.toYRot()));
        renderObj("vault_frame.obj",id("textures/models/doors/vault/vault_frame.png"),ps,b,l,o);
        ps.translate(-1,0,0); renderObj("vault_teeth.obj",id("textures/models/misc/universaldark.png"),ps,b,l,o); ps.translate(1,0,0);
        ps.translate(0,-2.5,0);
        double time=p*6000.0; double x=Math.max(0,Math.min(1,time/2000.0))+0.0005; double z=time<2000?0:Math.max(0,Math.min(5,(time-2000)/800.0)); double roll=z/(4.5*Math.PI)*360.0;
        ps.translate(-x,0,z); ps.translate(0,5,0); ps.mulPose(Axis.XP.rotationDegrees((float)roll)); ps.translate(0,-2.5,0);
        renderObj("vault_cog.obj",id("textures/models/doors/vault/vault_cog_1.png"),ps,b,l,o);
        renderObj("vault_label.obj",id("textures/models/doors/vault/vault_label_1.png"),ps,b,l,o);
        ps.popPose();
    }

    private void renderObj(String file,ResourceLocation texture,PoseStack ps,MultiBufferSource b,int l,int o) {
        LegacyObjModel m=obj.computeIfAbsent(file,k->LegacyObjModel.load(id("models/"+k)));
        VertexConsumer out=b.getBuffer(RenderType.entityCutoutNoCull(texture)); m.renderAllRaw(ps.last(),out,l,o);
    }

    private static ResourceLocation id(String path){return HbmDoorsPort.id(path);}

    @Override public AABB getRenderBoundingBox(SpecialDoorBlockEntity d){
        BlockPos p=d.getBlockPos(); return switch(d.type()){
            case TRANSITION_SEAL->new AABB(p.getX()-15,p.getY()-2,p.getZ()-15,p.getX()+16,p.getY()+28,p.getZ()+16);
            case SILO_HATCH->new AABB(p.getX()-10,p.getY()-3,p.getZ()-10,p.getX()+11,p.getY()+8,p.getZ()+11);
            case SLIDING_BLAST_DOOR->new AABB(p.getX()-6,p.getY()-2,p.getZ()-6,p.getX()+7,p.getY()+8,p.getZ()+7);
            case VAULT_DOOR->new AABB(p.getX()-8,p.getY()-4,p.getZ()-8,p.getX()+9,p.getY()+10,p.getZ()+9);
            case BLAST_DOOR->new AABB(p.getX()-3,p.getY()-2,p.getZ()-3,p.getX()+4,p.getY()+10,p.getZ()+4);
        };}
}
