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
 * Restores the empty-hand access behavior from the pinned HBM 1.12.2 source.
 *
 * In that fork, BlockDoorGeneric and the Blast/Vault/Silo blocks route player activation
 * through TileEntityLockableBase#canAccess. That base implementation does not grant access
 * to an empty hand. The normal Sliding Blast Door is the exception: its tile entity overrides
 * canAccess and explicitly allows interaction while it is not locked.
 *
 * The standalone port does not yet carry HBM's key/pin/lockpick items, so the legacy
 * access-controlled doors remain operable through redstone while empty-hand activation is denied.
 */
@EventBusSubscriber(modid = HbmDoorsPort.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ManualDoorAccessEvents {
    private ManualDoorAccessEvents() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().getItemInHand(event.getHand()).isEmpty()) return;

        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        boolean denyEmptyHand = block instanceof LegacyDoorBlock
                || block instanceof RoundAirlockDoorBlock
                || (block instanceof SpecialDoorBlock special
                    && special.type() != SpecialDoorType.SLIDING_BLAST_DOOR);

        if (denyEmptyHand) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
