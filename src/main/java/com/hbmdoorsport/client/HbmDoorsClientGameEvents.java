package com.hbmdoorsport.client;

import com.hbmdoorsport.HbmDoorsPort;
import com.hbmdoorsport.client.sound.DoorSoundController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Game-bus client hooks, deliberately separate from mod-bus renderer registration. */
@EventBusSubscriber(modid = HbmDoorsPort.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class HbmDoorsClientGameEvents {
    private HbmDoorsClientGameEvents() { }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        DoorSoundController.tick();
    }
}
