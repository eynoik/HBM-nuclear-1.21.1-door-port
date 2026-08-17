package com.hbmdoorsport.client;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.block.RoundAirlockDoorBlock;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;
import com.hbmdoorsport.client.model.LegacyObjModel;
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

/**
 * Modern endpoint for the old HBM RenderDoorGeneric pipeline.
 * The OBJ and its named parts remain untouched.
 */
public final class RoundAirlockDoorRenderer implements BlockEntityRenderer<RoundAirlockDoorBlockEntity> {
    private static final ResourceLocation MODEL = HbmDoorsPort.id("models/doors/round_airlock_door.obj");
    private static final ResourceLocation TEXTURE = HbmDoorsPort.id("textures/models/doors/round_airlock_door.png");
    private LegacyObjModel model;

    public RoundAirlockDoorRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(RoundAirlockDoorBlockEntity door, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == null) model = LegacyObjModel.load(MODEL);

        Direction facing = door.getBlockState().getValue(RoundAirlockDoorBlock.FACING);
        float progress = door.getLegacyRenderProgress();

        poseStack.pushPose();
        // RenderDoorGeneric: translate(x+0.5, y, z+0.5)
        poseStack.translate(0.5, 0.0, 0.5);
        // Exact old metadata mapping: N=90, W=180, S=270, E=360 degrees.
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - facing.toYRot()));
        // DoorDecl.ROUND_AIRLOCK_DOOR#doOffsetTransform
        poseStack.translate(0.0, 0.0, 0.5);

        VertexConsumer out = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // Keep the original OBJ group order: frame, doorRight, doorLeft.
        model.renderGroup("frame", 0.0F, pose, out, packedLight, packedOverlay);
        // Exact DoorDecl translations: +/- 1.5 * getNormTime(openTicks)
        model.renderGroup("doorRight", -1.5F * progress, pose, out, packedLight, packedOverlay);
        model.renderGroup("doorLeft", +1.5F * progress, pose, out, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RoundAirlockDoorBlockEntity door) {
        BlockPos p = door.getBlockPos();
        // Conservative box covering the full 4x4 model in every horizontal orientation.
        return new AABB(p.getX() - 3, p.getY(), p.getZ() - 3,
                p.getX() + 4, p.getY() + 5, p.getZ() + 4);
    }
}
