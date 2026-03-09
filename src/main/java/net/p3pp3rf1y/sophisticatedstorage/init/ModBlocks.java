package net.p3pp3rf1y.sophisticatedstorage.init;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.*;
import net.p3pp3rf1y.sophisticatedstorage.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.item.*;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModBlocks {
	private static final String LIMITED_BARREL_NAME = "limited_barrel";

	private ModBlocks() {
	}

	public static final TagKey<Item> BASE_TIER_WOODEN_STORAGE_TAG = TagKey.create(Registries.ITEM, SophisticatedStorage.getRL("base_tier_wooden_storage"));
	public static final TagKey<Item> ALL_STORAGE_TAG = TagKey.create(Registries.ITEM, SophisticatedStorage.getRL("all_storage"));

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SophisticatedStorage.MOD_ID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SophisticatedStorage.MOD_ID);
	private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, SophisticatedStorage.MOD_ID);

	private static final String BARREL_REG_NAME = "barrel";
	public static final Supplier<BarrelBlock> BARREL = BLOCKS.registerBlock(BARREL_REG_NAME, properties -> new BarrelBlock(Config.SERVER.woodBarrel.inventorySlotCount, Config.SERVER.woodBarrel.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> COPPER_BARREL = BLOCKS.registerBlock("copper_barrel", properties -> new BarrelBlock(Config.SERVER.copperBarrel.inventorySlotCount, Config.SERVER.copperBarrel.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> IRON_BARREL = BLOCKS.registerBlock("iron_barrel", properties -> new BarrelBlock(Config.SERVER.ironBarrel.inventorySlotCount, Config.SERVER.ironBarrel.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> GOLD_BARREL = BLOCKS.registerBlock("gold_barrel", properties -> new BarrelBlock(Config.SERVER.goldBarrel.inventorySlotCount, Config.SERVER.goldBarrel.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> DIAMOND_BARREL = BLOCKS.registerBlock("diamond_barrel", properties -> new BarrelBlock(Config.SERVER.diamondBarrel.inventorySlotCount, Config.SERVER.diamondBarrel.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> NETHERITE_BARREL = BLOCKS.registerBlock("netherite_barrel", properties -> new BarrelBlock(Config.SERVER.netheriteBarrel.inventorySlotCount, Config.SERVER.netheriteBarrel.upgradeSlotCount, 1200, properties));
	public static final DeferredHolder<Item, BlockItem> BARREL_ITEM = ITEMS.registerItem(BARREL_REG_NAME, properties -> new BarrelBlockItem(BARREL.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> COPPER_BARREL_ITEM = ITEMS.registerItem("copper_barrel", properties -> new BarrelBlockItem(COPPER_BARREL.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> IRON_BARREL_ITEM = ITEMS.registerItem("iron_barrel", properties -> new BarrelBlockItem(IRON_BARREL.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> GOLD_BARREL_ITEM = ITEMS.registerItem("gold_barrel", properties -> new BarrelBlockItem(GOLD_BARREL.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> DIAMOND_BARREL_ITEM = ITEMS.registerItem("diamond_barrel", properties -> new BarrelBlockItem(DIAMOND_BARREL.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> NETHERITE_BARREL_ITEM = ITEMS.registerItem("netherite_barrel", properties -> new BarrelBlockItem(NETHERITE_BARREL.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	private static final String LIMITED_BARREL_REG_NAME = LIMITED_BARREL_NAME;
	public static final Supplier<BarrelBlock> LIMITED_BARREL_1 = BLOCKS.registerBlock("limited_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.limitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel1.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_COPPER_BARREL_1 = BLOCKS.registerBlock("limited_copper_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.copperLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel1.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_IRON_BARREL_1 = BLOCKS.registerBlock("limited_iron_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.ironLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel1.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_GOLD_BARREL_1 = BLOCKS.registerBlock("limited_gold_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.goldLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel1.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_DIAMOND_BARREL_1 = BLOCKS.registerBlock("limited_diamond_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.diamondLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel1.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_NETHERITE_BARREL_1 = BLOCKS.registerBlock("limited_netherite_barrel_1", properties -> new LimitedBarrelBlock(1, Config.SERVER.netheriteLimitedBarrel1.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel1.upgradeSlotCount, 1200, properties));
	public static final Supplier<BlockItem> LIMITED_BARREL_1_ITEM = ITEMS.registerItem("limited_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_BARREL_1.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_IRON_BARREL_1_ITEM = ITEMS.registerItem("limited_iron_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_IRON_BARREL_1.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_COPPER_BARREL_1_ITEM = ITEMS.registerItem("limited_copper_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_COPPER_BARREL_1.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_GOLD_BARREL_1_ITEM = ITEMS.registerItem("limited_gold_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_GOLD_BARREL_1.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_DIAMOND_BARREL_1_ITEM = ITEMS.registerItem("limited_diamond_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_DIAMOND_BARREL_1.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_NETHERITE_BARREL_1_ITEM = ITEMS.registerItem("limited_netherite_barrel_1", properties -> new LimitedBarrelBlockItem(LIMITED_NETHERITE_BARREL_1.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	public static final Supplier<BarrelBlock> LIMITED_BARREL_2 = BLOCKS.registerBlock("limited_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.limitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel2.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_COPPER_BARREL_2 = BLOCKS.registerBlock("limited_copper_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.copperLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel2.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_IRON_BARREL_2 = BLOCKS.registerBlock("limited_iron_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.ironLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel2.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_GOLD_BARREL_2 = BLOCKS.registerBlock("limited_gold_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.goldLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel2.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_DIAMOND_BARREL_2 = BLOCKS.registerBlock("limited_diamond_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.diamondLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel2.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_NETHERITE_BARREL_2 = BLOCKS.registerBlock("limited_netherite_barrel_2", properties -> new LimitedBarrelBlock(2, Config.SERVER.netheriteLimitedBarrel2.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel2.upgradeSlotCount, 1200, properties));
	public static final Supplier<BlockItem> LIMITED_BARREL_2_ITEM = ITEMS.registerItem("limited_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_BARREL_2.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_COPPER_BARREL_2_ITEM = ITEMS.registerItem("limited_copper_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_COPPER_BARREL_2.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_IRON_BARREL_2_ITEM = ITEMS.registerItem("limited_iron_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_IRON_BARREL_2.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_GOLD_BARREL_2_ITEM = ITEMS.registerItem("limited_gold_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_GOLD_BARREL_2.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_DIAMOND_BARREL_2_ITEM = ITEMS.registerItem("limited_diamond_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_DIAMOND_BARREL_2.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_NETHERITE_BARREL_2_ITEM = ITEMS.registerItem("limited_netherite_barrel_2", properties -> new LimitedBarrelBlockItem(LIMITED_NETHERITE_BARREL_2.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	public static final Supplier<BarrelBlock> LIMITED_BARREL_3 = BLOCKS.registerBlock("limited_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.limitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel3.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_COPPER_BARREL_3 = BLOCKS.registerBlock("limited_copper_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.copperLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel3.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_IRON_BARREL_3 = BLOCKS.registerBlock("limited_iron_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.ironLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel3.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_GOLD_BARREL_3 = BLOCKS.registerBlock("limited_gold_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.goldLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel3.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_DIAMOND_BARREL_3 = BLOCKS.registerBlock("limited_diamond_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.diamondLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel3.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_NETHERITE_BARREL_3 = BLOCKS.registerBlock("limited_netherite_barrel_3", properties -> new LimitedBarrelBlock(3, Config.SERVER.netheriteLimitedBarrel3.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel3.upgradeSlotCount, 1200, properties));
	public static final Supplier<BlockItem> LIMITED_BARREL_3_ITEM = ITEMS.registerItem("limited_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_BARREL_3.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_COPPER_BARREL_3_ITEM = ITEMS.registerItem("limited_copper_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_COPPER_BARREL_3.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_IRON_BARREL_3_ITEM = ITEMS.registerItem("limited_iron_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_IRON_BARREL_3.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_GOLD_BARREL_3_ITEM = ITEMS.registerItem("limited_gold_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_GOLD_BARREL_3.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_DIAMOND_BARREL_3_ITEM = ITEMS.registerItem("limited_diamond_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_DIAMOND_BARREL_3.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_NETHERITE_BARREL_3_ITEM = ITEMS.registerItem("limited_netherite_barrel_3", properties -> new LimitedBarrelBlockItem(LIMITED_NETHERITE_BARREL_3.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	public static final Supplier<BarrelBlock> LIMITED_BARREL_4 = BLOCKS.registerBlock("limited_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.limitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.limitedBarrel4.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_COPPER_BARREL_4 = BLOCKS.registerBlock("limited_copper_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.copperLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.copperLimitedBarrel4.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_IRON_BARREL_4 = BLOCKS.registerBlock("limited_iron_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.ironLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.ironLimitedBarrel4.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_GOLD_BARREL_4 = BLOCKS.registerBlock("limited_gold_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.goldLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.goldLimitedBarrel4.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_DIAMOND_BARREL_4 = BLOCKS.registerBlock("limited_diamond_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.diamondLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.diamondLimitedBarrel4.upgradeSlotCount, 0, properties));
	public static final Supplier<BarrelBlock> LIMITED_NETHERITE_BARREL_4 = BLOCKS.registerBlock("limited_netherite_barrel_4", properties -> new LimitedBarrelBlock(4, Config.SERVER.netheriteLimitedBarrel4.baseSlotLimitMultiplier, Config.SERVER.netheriteLimitedBarrel4.upgradeSlotCount, 1200, properties));
	public static final Supplier<BlockItem> LIMITED_BARREL_4_ITEM = ITEMS.registerItem("limited_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_BARREL_4.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_COPPER_BARREL_4_ITEM = ITEMS.registerItem("limited_copper_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_COPPER_BARREL_4.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_IRON_BARREL_4_ITEM = ITEMS.registerItem("limited_iron_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_IRON_BARREL_4.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_GOLD_BARREL_4_ITEM = ITEMS.registerItem("limited_gold_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_GOLD_BARREL_4.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_DIAMOND_BARREL_4_ITEM = ITEMS.registerItem("limited_diamond_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_DIAMOND_BARREL_4.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> LIMITED_NETHERITE_BARREL_4_ITEM = ITEMS.registerItem("limited_netherite_barrel_4", properties -> new LimitedBarrelBlockItem(LIMITED_NETHERITE_BARREL_4.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	private static final String CHEST_REG_NAME = "chest";
	public static final Supplier<ChestBlock> CHEST = BLOCKS.registerBlock(CHEST_REG_NAME, properties -> new ChestBlock(Config.SERVER.woodChest.inventorySlotCount, Config.SERVER.woodChest.upgradeSlotCount, properties));
	public static final Supplier<ChestBlock> COPPER_CHEST = BLOCKS.registerBlock("copper_chest", properties -> new ChestBlock(Config.SERVER.copperChest.inventorySlotCount, Config.SERVER.copperChest.upgradeSlotCount, properties));
	public static final Supplier<ChestBlock> IRON_CHEST = BLOCKS.registerBlock("iron_chest", properties -> new ChestBlock(Config.SERVER.ironChest.inventorySlotCount, Config.SERVER.ironChest.upgradeSlotCount, properties));
	public static final Supplier<ChestBlock> GOLD_CHEST = BLOCKS.registerBlock("gold_chest", properties -> new ChestBlock(Config.SERVER.goldChest.inventorySlotCount, Config.SERVER.goldChest.upgradeSlotCount, properties));
	public static final Supplier<ChestBlock> DIAMOND_CHEST = BLOCKS.registerBlock("diamond_chest", properties -> new ChestBlock(Config.SERVER.diamondChest.inventorySlotCount, Config.SERVER.diamondChest.upgradeSlotCount, properties));
	public static final Supplier<ChestBlock> NETHERITE_CHEST = BLOCKS.registerBlock("netherite_chest", properties -> new ChestBlock(Config.SERVER.netheriteChest.inventorySlotCount, Config.SERVER.netheriteChest.upgradeSlotCount, 1200, properties));
	public static final DeferredHolder<Item, BlockItem> CHEST_ITEM = ITEMS.registerItem(CHEST_REG_NAME, properties -> new ChestBlockItem(CHEST.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> COPPER_CHEST_ITEM = ITEMS.registerItem("copper_chest", properties -> new ChestBlockItem(COPPER_CHEST.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> IRON_CHEST_ITEM = ITEMS.registerItem("iron_chest", properties -> new ChestBlockItem(IRON_CHEST.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> GOLD_CHEST_ITEM = ITEMS.registerItem("gold_chest", properties -> new ChestBlockItem(GOLD_CHEST.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> DIAMOND_CHEST_ITEM = ITEMS.registerItem("diamond_chest", properties -> new ChestBlockItem(DIAMOND_CHEST.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> NETHERITE_CHEST_ITEM = ITEMS.registerItem("netherite_chest", properties -> new ChestBlockItem(NETHERITE_CHEST.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	private static final String SHULKER_BOX_REG_NAME = "shulker_box";
	public static final Supplier<ShulkerBoxBlock> SHULKER_BOX = BLOCKS.registerBlock(SHULKER_BOX_REG_NAME, properties -> new ShulkerBoxBlock(Config.SERVER.shulkerBox.inventorySlotCount, Config.SERVER.shulkerBox.upgradeSlotCount, properties));
	public static final Supplier<ShulkerBoxBlock> COPPER_SHULKER_BOX = BLOCKS.registerBlock("copper_shulker_box", properties -> new ShulkerBoxBlock(Config.SERVER.copperShulkerBox.inventorySlotCount, Config.SERVER.copperShulkerBox.upgradeSlotCount, properties));
	public static final Supplier<ShulkerBoxBlock> IRON_SHULKER_BOX = BLOCKS.registerBlock("iron_shulker_box", properties -> new ShulkerBoxBlock(Config.SERVER.ironShulkerBox.inventorySlotCount, Config.SERVER.ironShulkerBox.upgradeSlotCount, properties));
	public static final Supplier<ShulkerBoxBlock> GOLD_SHULKER_BOX = BLOCKS.registerBlock("gold_shulker_box", properties -> new ShulkerBoxBlock(Config.SERVER.goldShulkerBox.inventorySlotCount, Config.SERVER.goldShulkerBox.upgradeSlotCount, properties));
	public static final Supplier<ShulkerBoxBlock> DIAMOND_SHULKER_BOX = BLOCKS.registerBlock("diamond_shulker_box", properties -> new ShulkerBoxBlock(Config.SERVER.diamondShulkerBox.inventorySlotCount, Config.SERVER.diamondShulkerBox.upgradeSlotCount, properties));
	public static final Supplier<ShulkerBoxBlock> NETHERITE_SHULKER_BOX = BLOCKS.registerBlock("netherite_shulker_box", properties -> new ShulkerBoxBlock(Config.SERVER.netheriteShulkerBox.inventorySlotCount, Config.SERVER.netheriteShulkerBox.upgradeSlotCount, 1200, properties));
	public static final DeferredHolder<Item, BlockItem> SHULKER_BOX_ITEM = ITEMS.registerItem(SHULKER_BOX_REG_NAME, properties -> new ShulkerBoxItem(SHULKER_BOX.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> COPPER_SHULKER_BOX_ITEM = ITEMS.registerItem("copper_shulker_box", properties -> new ShulkerBoxItem(COPPER_SHULKER_BOX.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> IRON_SHULKER_BOX_ITEM = ITEMS.registerItem("iron_shulker_box", properties -> new ShulkerBoxItem(IRON_SHULKER_BOX.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> GOLD_SHULKER_BOX_ITEM = ITEMS.registerItem("gold_shulker_box", properties -> new ShulkerBoxItem(GOLD_SHULKER_BOX.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> DIAMOND_SHULKER_BOX_ITEM = ITEMS.registerItem("diamond_shulker_box", properties -> new ShulkerBoxItem(DIAMOND_SHULKER_BOX.get(), properties.useBlockDescriptionPrefix()));
	public static final Supplier<BlockItem> NETHERITE_SHULKER_BOX_ITEM = ITEMS.registerItem("netherite_shulker_box", properties -> new ShulkerBoxItem(NETHERITE_SHULKER_BOX.get(), properties.useBlockDescriptionPrefix().fireResistant()));

	private static final String CONTROLLER_REG_NAME = "controller";
	public static final Supplier<ControllerBlock> CONTROLLER = BLOCKS.registerBlock(CONTROLLER_REG_NAME, ControllerBlock::new);
	private static final String STORAGE_LINK_REG_NAME = "storage_link";
	public static final Supplier<StorageLinkBlock> STORAGE_LINK = BLOCKS.registerBlock(STORAGE_LINK_REG_NAME, StorageLinkBlock::new);
	public static final DeferredHolder<Item, BlockItem> CONTROLLER_ITEM = ITEMS.registerItem(CONTROLLER_REG_NAME, properties -> new BlockItemBase(CONTROLLER.get(), properties.useBlockDescriptionPrefix()) {
		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
			StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
		}
	});
	public static final Supplier<BlockItem> STORAGE_LINK_ITEM = ITEMS.registerItem(STORAGE_LINK_REG_NAME, properties -> new BlockItemBase(STORAGE_LINK.get(), properties.useBlockDescriptionPrefix()){
		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
			super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
			StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
		}
	});
	public static final String STORAGE_IO_REG_NAME = "storage_io";
	public static final Supplier<StorageIOBlock> STORAGE_IO = BLOCKS.registerBlock(STORAGE_IO_REG_NAME, StorageIOBlock::new);
	public static final String STORAGE_INPUT_REG_NAME = "storage_input";
	public static final Supplier<StorageIOBlock> STORAGE_INPUT = BLOCKS.registerBlock(STORAGE_INPUT_REG_NAME, properties -> new StorageIOBlock(properties) {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new StorageInputBlockEntity(pos, state);
		}
	});
	public static final String STORAGE_OUTPUT_REG_NAME = "storage_output";
	public static final Supplier<StorageIOBlock> STORAGE_OUTPUT = BLOCKS.registerBlock(STORAGE_OUTPUT_REG_NAME, properties -> new StorageIOBlock(properties) {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new StorageOutputBlockEntity(pos, state);
		}
	});
	public static final Map<WoodType, Supplier<StorageConnectorBlock>> STORAGE_CONNECTOR_BLOCKS;

	public static final Supplier<BlockItem> STORAGE_IO_ITEM = ITEMS.registerItem(STORAGE_IO_REG_NAME, properties -> new BlockItemBase(STORAGE_IO.get(), properties.useBlockDescriptionPrefix()) {
		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
			StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
		}
	});
	public static final Supplier<BlockItem> STORAGE_INPUT_ITEM = ITEMS.registerItem(STORAGE_INPUT_REG_NAME, properties -> new BlockItemBase(STORAGE_INPUT.get(), properties.useBlockDescriptionPrefix()) {
		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
			StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
		}
	});
	public static final Supplier<BlockItem> STORAGE_OUTPUT_ITEM = ITEMS.registerItem(STORAGE_OUTPUT_REG_NAME, properties -> new BlockItemBase(STORAGE_OUTPUT.get(), properties.useBlockDescriptionPrefix()) {
		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
			StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);
		}
	});
	public static final Map<WoodType, Supplier<BlockItem>> STORAGE_CONNECTOR_ITEMS;

	static {
		ImmutableMap.Builder<WoodType, Supplier<StorageConnectorBlock>> blockBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<WoodType, Supplier<BlockItem>> itemBuilder = ImmutableMap.builder();
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> {
			String registryName = woodType.name().toLowerCase(Locale.ROOT) + "_storage_connector";
			DeferredHolder<Block, StorageConnectorBlock> blockHolder = BLOCKS.registerBlock(registryName, StorageConnectorBlock::new);
			blockBuilder.put(woodType, blockHolder);
			itemBuilder.put(woodType, ITEMS.registerItem(registryName, properties -> new StorageConnectorBlockItem(blockHolder.get(), properties.useBlockDescriptionPrefix())));
		});

		STORAGE_CONNECTOR_BLOCKS = blockBuilder.build();
		STORAGE_CONNECTOR_ITEMS = itemBuilder.build();
	}

	public static final Supplier<DecorationTableBlock> DECORATION_TABLE = BLOCKS.registerBlock("decoration_table", DecorationTableBlock::new);

	public static final Supplier<BlockItem> DECORATION_TABLE_ITEM = ITEMS.registerItem("decoration_table", properties -> new BlockItemBase(DECORATION_TABLE.get(), properties.useBlockDescriptionPrefix()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<BarrelBlockEntity>> BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(BARREL_REG_NAME, () ->
			new BlockEntityType<>(BarrelBlockEntity::new, BARREL.get(), COPPER_BARREL.get(), IRON_BARREL.get(), GOLD_BARREL.get(), DIAMOND_BARREL.get(), NETHERITE_BARREL.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<LimitedBarrelBlockEntity>> LIMITED_BARREL_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(LIMITED_BARREL_REG_NAME, () ->
			new BlockEntityType<>(LimitedBarrelBlockEntity::new,
					LIMITED_BARREL_1.get(), LIMITED_COPPER_BARREL_1.get(), LIMITED_IRON_BARREL_1.get(), LIMITED_GOLD_BARREL_1.get(), LIMITED_DIAMOND_BARREL_1.get(), LIMITED_NETHERITE_BARREL_1.get(),
					LIMITED_BARREL_2.get(), LIMITED_COPPER_BARREL_2.get(), LIMITED_IRON_BARREL_2.get(), LIMITED_GOLD_BARREL_2.get(), LIMITED_DIAMOND_BARREL_2.get(), LIMITED_NETHERITE_BARREL_2.get(),
					LIMITED_BARREL_3.get(), LIMITED_COPPER_BARREL_3.get(), LIMITED_IRON_BARREL_3.get(), LIMITED_GOLD_BARREL_3.get(), LIMITED_DIAMOND_BARREL_3.get(), LIMITED_NETHERITE_BARREL_3.get(),
					LIMITED_BARREL_4.get(), LIMITED_COPPER_BARREL_4.get(), LIMITED_IRON_BARREL_4.get(), LIMITED_GOLD_BARREL_4.get(), LIMITED_DIAMOND_BARREL_4.get(), LIMITED_NETHERITE_BARREL_4.get()
			));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<ChestBlockEntity>> CHEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(CHEST_REG_NAME, () ->
			new BlockEntityType<>(ChestBlockEntity::new, CHEST.get(), COPPER_CHEST.get(), IRON_CHEST.get(), GOLD_CHEST.get(), DIAMOND_CHEST.get(), NETHERITE_CHEST.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<ShulkerBoxBlockEntity>> SHULKER_BOX_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(SHULKER_BOX_REG_NAME, () ->
			new BlockEntityType<>(ShulkerBoxBlockEntity::new, SHULKER_BOX.get(), COPPER_SHULKER_BOX.get(), IRON_SHULKER_BOX.get(), GOLD_SHULKER_BOX.get(), DIAMOND_SHULKER_BOX.get(), NETHERITE_SHULKER_BOX.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<ControllerBlockEntity>> CONTROLLER_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(CONTROLLER_REG_NAME, () ->
			new BlockEntityType<>(ControllerBlockEntity::new, CONTROLLER.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<StorageLinkBlockEntity>> STORAGE_LINK_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_LINK_REG_NAME, () ->
			new BlockEntityType<>(StorageLinkBlockEntity::new, STORAGE_LINK.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<StorageIOBlockEntity>> STORAGE_IO_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_IO_REG_NAME, () ->
			new BlockEntityType<>(StorageIOBlockEntity::new, STORAGE_IO.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<StorageInputBlockEntity>> STORAGE_INPUT_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_INPUT_REG_NAME, () ->
			new BlockEntityType<>(StorageInputBlockEntity::new, STORAGE_INPUT.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<StorageOutputBlockEntity>> STORAGE_OUTPUT_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(STORAGE_OUTPUT_REG_NAME, () ->
			new BlockEntityType<>(StorageOutputBlockEntity::new, STORAGE_OUTPUT.get()));

	@SuppressWarnings("ConstantConditions") //no datafixer type needed
	public static final Supplier<BlockEntityType<StorageConnectorBlockEntity>> STORAGE_CONNECTOR_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("storage_connector", () ->
			new BlockEntityType<>(StorageConnectorBlockEntity::new, STORAGE_CONNECTOR_BLOCKS.values().stream().map(Supplier::get).toArray(StorageConnectorBlock[]::new)));

	public static final Supplier<BlockEntityType<DecorationTableBlockEntity>> DECORATION_TABLE_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("decoration_table", () ->
			new BlockEntityType<>(DecorationTableBlockEntity::new, DECORATION_TABLE.get()));

	public static final Supplier<MenuType<StorageContainerMenu>> STORAGE_CONTAINER_TYPE = MENU_TYPES.register("storage",
			() -> IMenuTypeExtension.create(StorageContainerMenu::fromBuffer));

	public static final Supplier<MenuType<LimitedBarrelContainerMenu>> LIMITED_BARREL_CONTAINER_TYPE = MENU_TYPES.register(LIMITED_BARREL_NAME,
			() -> IMenuTypeExtension.create(LimitedBarrelContainerMenu::fromBuffer));

	public static final Supplier<MenuType<StorageSettingsContainerMenu>> SETTINGS_CONTAINER_TYPE = MENU_TYPES.register("settings",
			() -> IMenuTypeExtension.create(StorageSettingsContainerMenu::fromBuffer));

	public static final Supplier<MenuType<LimitedBarrelSettingsContainerMenu>> LIMITED_BARREL_SETTINGS_CONTAINER_TYPE = MENU_TYPES.register("limited_barrel_settings",
			() -> IMenuTypeExtension.create(LimitedBarrelSettingsContainerMenu::fromBuffer));

	public static final Supplier<MenuType<DecorationTableMenu>> DECORATION_TABLE_CONTAINER_TYPE = MENU_TYPES.register("decoration_table",
			() -> IMenuTypeExtension.create(DecorationTableMenu::fromBuffer));

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, SophisticatedStorage.MOD_ID);
	public static final Supplier<CustomRecipe.Serializer<StorageDyeRecipe>> STORAGE_DYE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_dye", () -> new CustomRecipe.Serializer<>(StorageDyeRecipe::new));
	public static final Supplier<RecipeSerializer<StorageTierUpgradeRecipe>> STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_tier_upgrade", StorageTierUpgradeRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<DoubleChestTierUpgradeRecipe>> DOUBLE_CHEST_TIER_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("double_chest_tier_upgrade", DoubleChestTierUpgradeRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<ShulkerBoxFromVanillaShapelessRecipe>> SHULKER_BOX_FROM_VANILLA_SHAPELESS_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("shulker_box_from_vanilla_shapeless", ShulkerBoxFromVanillaShapelessRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<StorageTierUpgradeShapelessRecipe>> STORAGE_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("storage_tier_upgrade_shapeless", StorageTierUpgradeShapelessRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<DoubleChestTierUpgradeShapelessRecipe>> DOUBLE_CHEST_TIER_UPGRADE_SHAPELESS_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("double_chest_tier_upgrade_shapeless", DoubleChestTierUpgradeShapelessRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<ShulkerBoxFromChestRecipe>> SHULKER_BOX_FROM_CHEST_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("shulker_box_from_chest", ShulkerBoxFromChestRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<GenericWoodStorageRecipe>> GENERIC_WOOD_STORAGE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("generic_wood_storage", GenericWoodStorageRecipe.Serializer::new);
	public static final Supplier<CustomRecipe.Serializer<FlatTopBarrelToggleRecipe>> FLAT_TOP_BARREL_TOGGLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("flat_top_barrel_toggle", () -> new CustomRecipe.Serializer<>(FlatTopBarrelToggleRecipe::new));
	public static final Supplier<CustomRecipe.Serializer<BarrelMaterialRecipe>> BARREL_MATERIAL_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("barrel_material", () -> new CustomRecipe.Serializer<>(BarrelMaterialRecipe::new));
	private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, SophisticatedStorage.MOD_ID);
	public static final Supplier<IngredientType<BaseTierWoodenStorageIngredient>> BASE_TIER_WOODEN_STORAGE_INGREDIENT_TYPE = INGREDIENT_TYPES.register("base_tier_wooden_storage", () -> new IngredientType<>(BaseTierWoodenStorageIngredient.CODEC));

	public static void registerHandlers(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITY_TYPES.register(modBus);
		MENU_TYPES.register(modBus);
		RECIPE_SERIALIZERS.register(modBus);
		INGREDIENT_TYPES.register(modBus);
		modBus.addListener(ModBlocks::registerCapabilities);
		if (FMLEnvironment.dist.isClient()) {
			ModBlocksClient.init(modBus);
		}
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONTROLLER_BLOCK_ENTITY_TYPE.get(), ControllerBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHEST_BLOCK_ENTITY_TYPE.get(), ChestBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), ShulkerBoxBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LIMITED_BARREL_BLOCK_ENTITY_TYPE.get(), LimitedBarrelBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BARREL_BLOCK_ENTITY_TYPE.get(), BarrelBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STORAGE_IO_BLOCK_ENTITY_TYPE.get(), StorageIOBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STORAGE_INPUT_BLOCK_ENTITY_TYPE.get(), StorageInputBlockEntity::getExternalItemHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STORAGE_OUTPUT_BLOCK_ENTITY_TYPE.get(), StorageOutputBlockEntity::getExternalItemHandler);
	}

	public static void registerDispenseBehavior() {
		ShulkerBoxDispenseBehavior behavior = new ShulkerBoxDispenseBehavior();
		DispenserBlock.registerBehavior(SHULKER_BOX_ITEM.get(), behavior);
		DispenserBlock.registerBehavior(COPPER_SHULKER_BOX_ITEM.get(), behavior);
		DispenserBlock.registerBehavior(IRON_SHULKER_BOX_ITEM.get(), behavior);
		DispenserBlock.registerBehavior(GOLD_SHULKER_BOX_ITEM.get(), behavior);
		DispenserBlock.registerBehavior(DIAMOND_SHULKER_BOX_ITEM.get(), behavior);
		DispenserBlock.registerBehavior(NETHERITE_SHULKER_BOX_ITEM.get(), behavior);
	}

	public static void registerCauldronInteractions() {
		CauldronInteraction.WATER.map().put(BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(COPPER_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(IRON_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(GOLD_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(DIAMOND_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(NETHERITE_BARREL_ITEM.get(), BarrelCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.map().put(LIMITED_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_COPPER_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_COPPER_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_COPPER_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_COPPER_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_IRON_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_IRON_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_IRON_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_IRON_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_GOLD_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_GOLD_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_GOLD_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_GOLD_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_DIAMOND_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_DIAMOND_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_DIAMOND_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_DIAMOND_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_NETHERITE_BARREL_1_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_NETHERITE_BARREL_2_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_NETHERITE_BARREL_3_ITEM.get(), BarrelCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(LIMITED_NETHERITE_BARREL_4_ITEM.get(), BarrelCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.map().put(CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(COPPER_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(IRON_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(GOLD_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(DIAMOND_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(NETHERITE_CHEST_ITEM.get(), WoodStorageCauldronInteraction.INSTANCE);

		CauldronInteraction.WATER.map().put(SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(COPPER_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(IRON_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(GOLD_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(DIAMOND_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
		CauldronInteraction.WATER.map().put(NETHERITE_SHULKER_BOX_ITEM.get(), StorageCauldronInteraction.INSTANCE);
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class BarrelCauldronInteraction extends WoodStorageCauldronInteraction {
		private static final BarrelCauldronInteraction INSTANCE = new BarrelCauldronInteraction();

		@Override
		protected void removePaint(ItemStack stack) {
			super.removePaint(stack);
			BarrelBlockItem.removeMaterials(stack);
		}
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class WoodStorageCauldronInteraction extends StorageCauldronInteraction {
		private static final WoodStorageCauldronInteraction INSTANCE = new WoodStorageCauldronInteraction();

		@Override
		protected void removePaint(ItemStack stack) {
			super.removePaint(stack);
			if (WoodStorageBlockItem.getWoodType(stack).isEmpty()) {
				WoodStorageBlockItem.setWoodType(stack, WoodType.ACACIA);
			}
		}

		@Override
		protected boolean canRemovePaint(ItemStack stack) {
			return super.canRemovePaint(stack) && !WoodStorageBlockItem.isPacked(stack);
		}
	}

	@SuppressWarnings("java:S6548") //singleton is correct here
	public static class StorageCauldronInteraction implements CauldronInteraction {
		private static final StorageCauldronInteraction INSTANCE = new StorageCauldronInteraction();

		@Override
		public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
			if (canRemovePaint(stack)) {

				if (!level.isClientSide()) {
					removePaint(stack);
					LayeredCauldronBlock.lowerFillLevel(state, level, pos);
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		protected boolean canRemovePaint(ItemStack stack) {
			return stack.getItem() instanceof ITintableBlockItem;
		}

		protected void removePaint(ItemStack stack) {
			if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.removeMainColor(stack);
				tintableBlockItem.removeAccentColor(stack);
			}
		}
	}
}
