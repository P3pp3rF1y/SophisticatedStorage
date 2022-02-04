package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;

public class ModBlocks {
	private ModBlocks() {}

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REGISTRY_NAME = "barrel";

	public static final RegistryObject<Block> BARREL = BLOCKS.register(BARREL_REGISTRY_NAME, BarrelBlock::new);
	public static final RegistryObject<BlockItem> BARREL_ITEM = ITEMS.register(BARREL_REGISTRY_NAME, () -> new BlockItem(BARREL.get(), new Item.Properties().tab(SophisticatedStorage.CREATIVE_TAB)));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL_TILE_TYPE = BLOCK_ENTITIES.register(BARREL_REGISTRY_NAME, () ->
			BlockEntityType.Builder.of(BarrelBlockEntity::new, BARREL.get())
					.build(null));

	public static void registerHandlers(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
	}
}
