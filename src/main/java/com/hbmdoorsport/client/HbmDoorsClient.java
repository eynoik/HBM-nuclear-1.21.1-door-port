package com.hbmdoorsport.client;

import com.hbmdoorsport.HbmDoorsPort;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HbmDoorsClient {
    private HbmDoorsClient() { }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmDoorsPort.ROUND_AIRLOCK_DOOR_BE.get(), RoundAirlockDoorRenderer::new);
        event.registerBlockEntityRenderer(HbmDoorsPort.LEGACY_DOOR_BE.get(), LegacyDoorRenderer::new);
        event.registerBlockEntityRenderer(HbmDoorsPort.SPECIAL_DOOR_BE.get(), SpecialDoorRenderer::new);
    }
}
