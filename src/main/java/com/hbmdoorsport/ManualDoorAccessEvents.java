package com.hbmdoorsport;

import com.hbmdoorsport.block.LegacyDoorBlock;
import com.hbmdoorsport.block.RoundAirlockDoorBlock;
import com.hbmdoorsport.block.SpecialDoorBlock;
import com.hbmdoorsport.door.SpecialDoorType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Restores the direct player-access split from the pinned HBM 1.12.2 source.
 *
 * In that fork, BlockDoorGeneric and the Blast/Vault/Silo blocks route player activation
 * through TileEntityLockableBase#canAccess. The standalone port does not yet carry HBM's
 * key/pin/lockpick access items, so these doors must not be directly toggled by arbitrary
 * held items either. The normal Sliding Blast Door is the exception: its tile entity overrides
 * canAccess and explicitly allows direct interaction while it is not locked.
 *
 * Redstone remains available for the access-controlled doors.
 */
@EventBusSubscriber(modid = HbmDoorsPort.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ManualDoorAccessEvents {
    private ManualDoorAccessEvents() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        boolean denyDirectActivation = block instanceof LegacyDoorBlock
                || block instanceof RoundAirlockDoorBlock
                || (block instanceof SpecialDoorBlock special
                    && special.type() != SpecialDoorType.SLIDING_BLAST_DOOR);

        if (denyDirectActivation) {
            // Cancel the whole right-click block pipeline for these doors. This intentionally
            // blocks both empty-hand and held-item activation until the real HBM access items
            // are ported. Without this, any random item falls through to the block interaction
            // path and calls useWithoutItem(), reopening the same exploit through seeds/food/etc.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
