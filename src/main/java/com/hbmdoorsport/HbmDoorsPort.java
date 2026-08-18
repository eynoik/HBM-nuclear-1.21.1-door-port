package com.hbmdoorsport;

import com.hbmdoorsport.block.LegacyDoorBlock;
import com.hbmdoorsport.block.RoundAirlockDoorBlock;
import com.hbmdoorsport.block.SpecialDoorBlock;
import com.hbmdoorsport.blockentity.LegacyDoorBlockEntity;
import com.hbmdoorsport.blockentity.RoundAirlockDoorBlockEntity;
import com.hbmdoorsport.blockentity.SpecialDoorBlockEntity;
import com.hbmdoorsport.door.LegacyDoorType;
import com.hbmdoorsport.door.SpecialDoorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod(HbmDoorsPort.MODID)
public final class HbmDoorsPort {
    public static final String MODID = "hbmdoors";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<RoundAirlockDoorBlock> ROUND_AIRLOCK_DOOR = BLOCKS.registerBlock(
            "round_airlock_door", RoundAirlockDoorBlock::new, uniformDoorProperties());
    public static final DeferredItem<BlockItem> ROUND_AIRLOCK_DOOR_ITEM = ITEMS.register(
            "round_airlock_door", () -> new BlockItem(ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    private static final Map<LegacyDoorType, DeferredBlock<LegacyDoorBlock>> LEGACY_BLOCKS = new EnumMap<>(LegacyDoorType.class);
    private static final Map<LegacyDoorType, DeferredItem<BlockItem>> LEGACY_ITEMS = new EnumMap<>(LegacyDoorType.class);
    private static final Map<SpecialDoorType, DeferredBlock<SpecialDoorBlock>> SPECIAL_BLOCKS = new EnumMap<>(SpecialDoorType.class);
    private static final Map<SpecialDoorType, DeferredItem<BlockItem>> SPECIAL_ITEMS = new EnumMap<>(SpecialDoorType.class);

    static {
        for (LegacyDoorType type : LegacyDoorType.values()) {
            DeferredBlock<LegacyDoorBlock> block = BLOCKS.registerBlock(type.id(), p -> new LegacyDoorBlock(type, p), uniformDoorProperties());
            LEGACY_BLOCKS.put(type, block);
            LEGACY_ITEMS.put(type, ITEMS.register(type.id(), () -> new BlockItem(block.get(), new Item.Properties())));
        }
        for (SpecialDoorType type : SpecialDoorType.values()) {
            DeferredBlock<SpecialDoorBlock> block = BLOCKS.registerBlock(type.id(), p -> new SpecialDoorBlock(type, p), uniformDoorProperties());
            SPECIAL_BLOCKS.put(type, block);
            SPECIAL_ITEMS.put(type, ITEMS.register(type.id(), () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final Supplier<CreativeModeTab> HBM_DOORS_TAB = CREATIVE_TABS.register(
            "hbm_doors", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hbmdoors"))
                    .icon(() -> new ItemStack(ROUND_AIRLOCK_DOOR_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ROUND_AIRLOCK_DOOR_ITEM.get());
                        for (LegacyDoorType type : LegacyDoorType.values()) output.accept(itemFor(type).get());
                        for (SpecialDoorType type : SpecialDoorType.values()) output.accept(specialItemFor(type).get());
                    })
                    .build());

    public static final Supplier<BlockEntityType<RoundAirlockDoorBlockEntity>> ROUND_AIRLOCK_DOOR_BE = BLOCK_ENTITIES.register(
            "round_airlock_door", () -> BlockEntityType.Builder.of(RoundAirlockDoorBlockEntity::new, ROUND_AIRLOCK_DOOR.get()).build(null));

    public static final Supplier<BlockEntityType<LegacyDoorBlockEntity>> LEGACY_DOOR_BE = BLOCK_ENTITIES.register(
            "legacy_door", () -> BlockEntityType.Builder.of(LegacyDoorBlockEntity::new,
                    LEGACY_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<SpecialDoorBlockEntity>> SPECIAL_DOOR_BE = BLOCK_ENTITIES.register(
            "special_door", () -> BlockEntityType.Builder.of(SpecialDoorBlockEntity::new,
                    SPECIAL_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    // Original HBM door samples, only their registry names are modernized.
    public static final Supplier<SoundEvent> ROUND_AIRLOCK_MOVE = sound("round_airlock.move");
    public static final Supplier<SoundEvent> ROUND_AIRLOCK_STOP = sound("round_airlock.stop");
    public static final Supplier<SoundEvent> SLIDING_SEAL_MOVE = sound("sliding_seal.move");
    public static final Supplier<SoundEvent> SLIDING_SEAL_STOP = sound("sliding_seal.stop");
    public static final Supplier<SoundEvent> WGH_MOVE = sound("wgh.move");
    public static final Supplier<SoundEvent> WGH_STOP = sound("wgh.stop");
    public static final Supplier<SoundEvent> WGH_BIG_MOVE = sound("wgh_big.move");
    public static final Supplier<SoundEvent> WGH_BIG_STOP = sound("wgh_big.stop");
    public static final Supplier<SoundEvent> ALARM6 = sound("alarm6");
    public static final Supplier<SoundEvent> QE_OPENING = sound("qe.opening");
    public static final Supplier<SoundEvent> QE_OPENED = sound("qe.opened");
    public static final Supplier<SoundEvent> QE_SHUT = sound("qe.shut");
    public static final Supplier<SoundEvent> HATCH_OPEN = sound("hatch.open");
    public static final Supplier<SoundEvent> LEVER = sound("door.lever");

    // Bespoke legacy door sounds.
    public static final Supplier<SoundEvent> TRANSITION_SEAL_OPEN = sound("special.transition_seal");
    public static final Supplier<SoundEvent> REACTOR_START = sound("special.reactor_start");
    public static final Supplier<SoundEvent> REACTOR_STOP = sound("special.reactor_stop");
    public static final Supplier<SoundEvent> VAULT_SCRAPE = sound("special.vault_scrape");
    public static final Supplier<SoundEvent> VAULT_THUD = sound("special.vault_thud");
    public static final Supplier<SoundEvent> SILO_OPEN = sound("special.silo_open");
    public static final Supplier<SoundEvent> SILO_CLOSE = sound("special.silo_close");

    static {
        LegacyDoorType.SLIDING_SEAL.sounds(SLIDING_SEAL_MOVE, SLIDING_SEAL_MOVE, null, null, null, null);
        LegacyDoorType.SLIDING_GATE.sounds(SLIDING_SEAL_MOVE, SLIDING_SEAL_MOVE, null, null, SLIDING_SEAL_STOP, SLIDING_SEAL_STOP);
        LegacyDoorType.SECURE_ACCESS.sounds(null, null, ROUND_AIRLOCK_MOVE, null, ROUND_AIRLOCK_STOP, ROUND_AIRLOCK_STOP);
        LegacyDoorType.HATCH.sounds(HATCH_OPEN, HATCH_OPEN, null, null, null, null);
        LegacyDoorType.FIRE.sounds(null, null, WGH_MOVE, ALARM6, WGH_STOP, WGH_STOP);
        LegacyDoorType.QE_SLIDING.sounds(null, null, QE_OPENING, null, QE_OPENED, QE_SHUT);
        LegacyDoorType.QE_CONTAINMENT.sounds(null, null, WGH_MOVE, null, WGH_STOP, WGH_STOP);
        LegacyDoorType.WATER.sounds(LEVER, null, WGH_BIG_MOVE, null, WGH_BIG_STOP, LEVER);
        LegacyDoorType.LARGE_VEHICLE.sounds(null, null, ROUND_AIRLOCK_MOVE, null, ROUND_AIRLOCK_STOP, ROUND_AIRLOCK_STOP);
    }

    /** Shared durability for every ported HBM door; slightly tougher than vanilla obsidian. */
    private static BlockBehaviour.Properties uniformDoorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(55F, 1_300F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape();
    }

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    public HbmDoorsPort(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    public static DeferredBlock<LegacyDoorBlock> blockFor(LegacyDoorType type) { return LEGACY_BLOCKS.get(type); }
    public static DeferredItem<BlockItem> itemFor(LegacyDoorType type) { return LEGACY_ITEMS.get(type); }
    public static DeferredBlock<SpecialDoorBlock> specialBlockFor(SpecialDoorType type) { return SPECIAL_BLOCKS.get(type); }
    public static DeferredItem<BlockItem> specialItemFor(SpecialDoorType type) { return SPECIAL_ITEMS.get(type); }

    public static void playSpecialDoorSound(Level level, BlockPos pos, SpecialDoorType type, boolean opening, boolean start) {
        Supplier<SoundEvent> sound = null; float volume=1F, pitch=1F;
        switch(type) {
            case TRANSITION_SEAL -> { if(start&&opening){sound=TRANSITION_SEAL_OPEN;volume=6F;} }
            case BLAST_DOOR -> { sound=start?REACTOR_START:REACTOR_STOP; volume=.5F; pitch=start?.75F:1F; }
            case SLIDING_BLAST_DOOR -> {
                // HBM 1.12 used qe_sliding_opening while moving, then qe_sliding_opened/shut at the end.
                sound = start ? QE_OPENING : (opening ? QE_OPENED : QE_SHUT);
                volume = 2F;
            }
            case VAULT_DOOR -> { if(start){sound=opening?VAULT_SCRAPE:VAULT_THUD;} }
            case SILO_HATCH -> { if(start){sound=opening?SILO_OPEN:SILO_CLOSE;volume=opening?4F:3F;} }
        }
        if(sound!=null) level.playSound(null,pos,sound.get(),SoundSource.BLOCKS,volume,pitch);
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }
}
