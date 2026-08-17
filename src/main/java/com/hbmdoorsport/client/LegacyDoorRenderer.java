package com.hbmdoorsport.client;

import com.hbmdoorsport.block.LegacyDoorBlock;
import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import com.hbmdoorsport.client.model.LegacyObjModel;
import com.hbmdoorsport.door.LegacyDoorType;
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

import java.util.EnumMap;
import java.util.Map;

/** Modern renderer endpoint preserving the original OBJ groups and DoorDecl transform math. */
public final class LegacyDoorRenderer implements BlockEntityRenderer<LegacyDoorBlockEntity> {
    private final Map<LegacyDoorType, LegacyObjModel> models = new EnumMap<>(LegacyDoorType.class);

    public LegacyDoorRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(LegacyDoorBlockEntity door,float partialTick,PoseStack ps,MultiBufferSource buffers,int light,int overlay){
        LegacyDoorType t=door.type();
        LegacyObjModel model=models.computeIfAbsent(t,k->LegacyObjModel.load(k.model()));
        Direction facing=door.getBlockState().getValue(LegacyDoorBlock.FACING);
        float p=door.getLegacyRenderProgress();

        ps.pushPose();
        ps.translate(0.5,0,0.5);
        ps.mulPose(Axis.YP.rotationDegrees(270.0F-facing.toYRot()));
        applyDoorOffset(t,ps);

        switch(t){
            case SLIDING_SEAL, SLIDING_GATE -> {
                renderTranslated(model,"frame",0,0,0, t.texture(),ps,buffers,light,overlay,
                        neg(),pos(),neg(),pos(),neg(),0.5001F);
                float z=LegacyDoorType.smoothstep(p);
                renderTranslated(model,"door",0,0,z,t.texture(),ps,buffers,light,overlay,
                        neg(),pos(),neg(),pos(),neg(),0.5001F);
            }
            case SECURE_ACCESS -> {
                renderTranslated(model,"base",0,0,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),5F,neg(),pos());
                renderTranslated(model,"door",0,3.5F*p,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),5F,neg(),pos());
            }
            case HATCH -> renderHatch(model,t,p,ps,buffers,light,overlay);
            case FIRE -> {
                renderTranslated(model,"frame",0,0,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),3.0001F,neg(),pos());
                renderTranslated(model,"door",0,3F*p,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),3.0001F,neg(),pos());
            }
            case QE_SLIDING -> {
                renderRaw(model,"leftDoor",t.texture(),transform(ps,0,0,+p,0,0,0,0,0,0),buffers,light,overlay); pop(ps);
                renderRaw(model,"rightDoor",t.texture(),transform(ps,0,0,-p,0,0,0,0,0,0),buffers,light,overlay); pop(ps);
            }
            case QE_CONTAINMENT -> {
                renderTranslated(model,"frame",0,0,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),3.0001F,neg(),pos());
                renderTranslated(model,"door",0,3F*p,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),3.0001F,neg(),pos());
                renderTranslated(model,"decal",0,3F*p,0,t.decalTexture(),ps,buffers,light,overlay,neg(),pos(),neg(),3.0001F,neg(),pos());
            }
            case WATER -> renderWater(model,t,p,ps,buffers,light,overlay);
            case LARGE_VEHICLE -> {
                renderTranslated(model,"frame",0,0,0,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),pos(),-3.50001F,3.50001F);
                renderTranslated(model,"doorLeft",0,0,3F*p,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),pos(),-3.50001F,3.50001F);
                renderTranslated(model,"doorRight",0,0,-3F*p,t.texture(),ps,buffers,light,overlay,neg(),pos(),neg(),pos(),-3.50001F,3.50001F);
            }
        }
        ps.popPose();
    }

    private static void applyDoorOffset(LegacyDoorType t,PoseStack ps){
        switch(t){
            case SLIDING_SEAL,SLIDING_GATE -> ps.translate(0.375,0,0);
            case SECURE_ACCESS -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case HATCH -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case FIRE -> ps.translate(0,0,0.5);
            case QE_SLIDING -> ps.translate(0.4375,0,0.5);
            case QE_CONTAINMENT -> ps.translate(0.25,0,0);
            case WATER -> ps.translate(0.375,0,0);
            case LARGE_VEHICLE -> { }
        }
    }

    private static void renderHatch(LegacyObjModel model,LegacyDoorType t,float p,PoseStack ps,MultiBufferSource b,int l,int o){
        renderRawAtCurrent(model,"base",t.texture(),ps,b,l,o);
        float hatchAngle=LegacyDoorType.smoothstep(t.segment(p,15,30))*90F-90F;
        transform(ps,0,0,0,0,1.03157F,0.591647F,hatchAngle,0,0);
        renderRawAtCurrent(model,"hatch",t.texture(),ps,b,l,o);
        float spin=LegacyDoorType.smoothstep(t.segment(p,0,15))*360F;
        transform(ps,0,0,0,0,1.62322F,0.434233F,0,0,spin);
        renderRawAtCurrent(model,"spinny",t.texture(),ps,b,l,o);
        pop(ps); pop(ps);
    }

    private static void renderWater(LegacyObjModel model,LegacyDoorType t,float p,PoseStack ps,MultiBufferSource b,int l,int o){
        renderRawAtCurrent(model,"frame",t.texture(),ps,b,l,o);
        float first=LegacyDoorType.smoothstep(t.segment(p,0,30));
        float second=LegacyDoorType.smoothstep(t.segment(p,30,60));
        float doorRot=-134F*second;

        transform(ps,0,0,0,0.125F,1.5F,1.18F,0,doorRot,0);
        renderRawAtCurrent(model,"door",t.texture(),ps,b,l,o);
        transform(ps,0,0,0,0.041499F,0.571054F,-0.587849F,360F*first,0,0);
        renderRawAtCurrent(model,"spinny_lower",t.texture(),ps,b,l,o); pop(ps);
        transform(ps,0,0,0,0.041499F,2.43569F,-0.587849F,360F*first,0,0);
        renderRawAtCurrent(model,"spinny_upper",t.texture(),ps,b,l,o); pop(ps);
        pop(ps);

        transform(ps,0,0,0.4F*first,0.125F,1.5F,1.18F,0,doorRot,0);
        renderRawAtCurrent(model,"bolt",t.texture(),ps,b,l,o); pop(ps);
    }

    private static PoseStack transform(PoseStack ps,float tx,float ty,float tz,float ox,float oy,float oz,float rx,float ry,float rz){
        ps.pushPose(); ps.translate(ox,oy,oz);
        if(rx!=0)ps.mulPose(Axis.XP.rotationDegrees(rx));
        if(ry!=0)ps.mulPose(Axis.YP.rotationDegrees(ry));
        if(rz!=0)ps.mulPose(Axis.ZP.rotationDegrees(rz));
        ps.translate(-ox+tx,-oy+ty,-oz+tz); return ps;
    }
    private static void pop(PoseStack ps){ps.popPose();}

    private static void renderRaw(LegacyObjModel m,String g,ResourceLocation tex,PoseStack ps,MultiBufferSource b,int l,int o){
        renderRawAtCurrent(m,g,tex,ps,b,l,o);
    }
    private static void renderRawAtCurrent(LegacyObjModel m,String g,ResourceLocation tex,PoseStack ps,MultiBufferSource b,int l,int o){
        VertexConsumer out=b.getBuffer(RenderType.entityCutoutNoCull(tex)); m.renderGroupRaw(g,ps.last(),out,l,o);
    }
    private static void renderTranslated(LegacyObjModel m,String g,float dx,float dy,float dz,ResourceLocation tex,PoseStack ps,MultiBufferSource b,int l,int o,
                                         float minX,float maxX,float minY,float maxY,float minZ,float maxZ){
        VertexConsumer out=b.getBuffer(RenderType.entityCutoutNoCull(tex));
        m.renderGroupTranslatedClipped(g,dx,dy,dz,minX,maxX,minY,maxY,minZ,maxZ,ps.last(),out,l,o);
    }
    private static float neg(){return Float.NEGATIVE_INFINITY;} private static float pos(){return Float.POSITIVE_INFINITY;}

    @Override
    public AABB getRenderBoundingBox(LegacyDoorBlockEntity door){
        int[] d=door.type().dimensions(); BlockPos p=door.getBlockPos(); int r=Math.max(Math.max(d[4],d[5]),Math.max(d[2],d[3]))+5;
        return new AABB(p.getX()-r,p.getY()-2,p.getZ()-r,p.getX()+r+1,p.getY()+d[0]+6,p.getZ()+r+1);
    }
}
